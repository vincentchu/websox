package com.vincentchu.websox.websocket

import com.twitter.finagle.netty3.Conversions._
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.websocketx._
import java.util.UUID

class WebsocketHandler[A](mesg: MessageBijection[A]) extends SimpleChannelHandler {

  private[this] val socketId: SocketId = UUID.randomUUID.toString
  private[this] var handshakerFactory: Option[WebSocketServerHandshakerFactory] = None
  private[this] var handshaker: Option[WebSocketServerHandshaker] = None

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("** writeRequested")
    e.getMessage match {
      case m: String =>
        println("GOT String")
        ctx.getChannel.write(new TextWebSocketFrame(m))
      case resp: Message[A]      =>
        println("ENCODE TO TEXTFRAME")
        ctx.getChannel.write(mesg.invert(resp.message))
      case _ =>
        println("GOT OTHER")
        ctx.sendDownstream(e)
        println("setSuccess!!")
        e.getFuture.setSuccess()
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("** messageReceived")
    e.getMessage match {
      case httpReq: HttpRequest    => handleHandshake(ctx, httpReq)
      case wsFrame: WebSocketFrame => handleWebSocketReq(ctx, wsFrame)
      case x: Message[A]               =>
        println("** something else in ", ctx.getName, x, ctx.getChannel.isReadable, ctx.getChannel.isWritable)
        println("** pipeline", ctx.getPipeline.getNames)
//        Channels.fireMessageReceived(ctx, "str")
        ctx.sendUpstream(e)

      case y =>
        println("** something here", y)
        ctx.getChannel.close()

    }
  }

  private[this] def handleWebSocketReq(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    println("handle wsSocketRequest")
    frame match {
      case textFrame: TextWebSocketFrame =>
        ctx.getChannel.setReadable(false)
        val message = Message.fromDecodedMessage("socketId", mesg(textFrame))
        ctx.getChannel.setReadable(true)
        Channels.fireMessageReceived(ctx.getChannel, message)

      case pingFrame: PingWebSocketFrame =>
        ctx.getChannel.write(new PongWebSocketFrame(frame.getBinaryData))

      case closeFrame: CloseWebSocketFrame =>
        println("** closeFrame")
        handshaker.foreach { _.close(ctx.getChannel, closeFrame) }
    }
  }

  private[this] def handleHandshake(ctx: ChannelHandlerContext, req: HttpRequest) {
    setHandshakerFactory(req)
    handshaker match {
      case Some(wsHandshaker) =>
        try {
          wsHandshaker.handshake(ctx.getChannel, req).toTwitterFuture map { _ =>
            println("** handshake completed!")
          }
        } catch {
          case _: Exception => sendErrorAndClose(ctx)
        }

      case _ => sendErrorAndClose(ctx)
    }
  }

  private[this] def sendErrorAndClose(ctx: ChannelHandlerContext) {
    handshakerFactory foreach { factory =>
      factory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel)
      ctx.getChannel.close()
    }
  }

  private[this] def setHandshakerFactory(req: HttpRequest) {
    if (handshakerFactory.isEmpty) {
      val location = "ws://" + req.getHeader(HttpHeaders.Names.HOST) + "/"
      handshakerFactory = Option(new WebSocketServerHandshakerFactory(location, null, false))
      handshaker = handshakerFactory map { _.newHandshaker(req) }
    }
  }
}
