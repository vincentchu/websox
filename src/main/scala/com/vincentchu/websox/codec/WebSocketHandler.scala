package com.vincentchu.websox.codec

import com.twitter.finagle.netty3.Conversions._
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.websocketx._
import com.vincentchu.websox.websocket.{WebSocketService, Message, MessageBijection}
import scala.runtime.BoxedUnit

class WebSocketHandler[A](mesg: MessageBijection[A], service: WebSocketService[A]) extends SimpleChannelHandler {

  private var handshakerFactory: Option[WebSocketServerHandshakerFactory] = None
  private var handshaker: Option[WebSocketServerHandshaker] = None

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("** writeRequested")
    e.getMessage match {
      case _: BoxedUnit => println("** GOT UNIT")
      case resp: Message[A]      =>
        println("ENCODE TO TEXTFRAME")
        ctx.getChannel.write(mesg.invert(resp.message))
      case _ =>
        println("GOT OTHER")
        ctx.sendDownstream(e)
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("** messageReceived")
    e.getMessage match {
      case httpReq: HttpRequest    => handleHandshake(ctx, httpReq)
      case wsFrame: WebSocketFrame => handleWebSocketReq(ctx, wsFrame)
      case x: Message[A]               =>
        println("** something else", x)
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
        val message = Message.fromDecodedMessage("socketId", mesg(textFrame))
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
            service.registerSocket("socketId", Map.empty, ctx)
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
