package com.vincentchu.websox.websocket

import com.twitter.finagle.netty3.Conversions._
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.websocketx._
import java.util.UUID
import com.twitter.util.{Promise, Try, Future}

class WebsocketHandler[A](mesg: MessageBijection[A], service: WebsocketService[A]) extends SimpleChannelHandler {

  private[this] val socketId: SocketId = UUID.randomUUID.toString
  private[this] var handshakerFactory: Option[WebSocketServerHandshakerFactory] = None
  private[this] var handshaker: Option[WebSocketServerHandshaker] = None
  private[this] val websocket = new Promise[Websocket[A]]

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("** writeRequested")
    e.getMessage match {
      case httpResp: HttpResponse =>
        println("httpResponse")
        ctx.sendDownstream(e)
      case textFrame: TextWebSocketFrame =>
        println("textFrame")
        ctx.sendDownstream(e)
      case resp: A =>
        println("ENCODE TO TEXTFRAME")
        ctx.getChannel.write(mesg.invert(resp))
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
      case m: A => println("messageReceived: A")
        websocket foreach {
          _.sendUpstream(m)
        }

      case y =>
        println("** something here", y)
        ctx.getChannel.close()

    }
  }

  private[this] def handleWebSocketReq(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    println("handle wsSocketRequest")
    frame match {
      case textFrame: TextWebSocketFrame =>
        Channels.fireMessageReceived(ctx.getChannel, mesg(textFrame))

      case pingFrame: PingWebSocketFrame =>
        ctx.getChannel.write(new PongWebSocketFrame(frame.getBinaryData))

      case closeFrame: CloseWebSocketFrame =>
        println("** closeFrame")
        handshaker.foreach { _.close(ctx.getChannel, closeFrame) }
    }
  }

  private[this] def handleHandshake(ctx: ChannelHandlerContext, req: HttpRequest) {
    println("** handleHandshake")
    setHandshakerFactory(req)
    handshaker match {
      case Some(wsHandshaker) =>
        try {
          wsHandshaker.handshake(ctx.getChannel, req).toTwitterFuture map { _ =>
            println("** handshake completed!")
            setWebsocketAndRegister(ctx)
          }
        } catch {
          case _: Exception => sendErrorAndClose(ctx)
        }

      case _ => sendErrorAndClose(ctx)
    }
  }

  private[this] def setWebsocketAndRegister(ctx: ChannelHandlerContext) {
    val ws = new ChannelHandlerContextWebsocket[A](
      socketId,
      ctx,
      service.onMessage(socketId, _: A),
      downstreamCallback _,
      closeCallback _
    )

    websocket.setValue(ws)
    service.registerSocket(ws)
  }

  private[this] def closeCallback() {
    println("** closeCallback")
  }

  private[this] def downstreamCallback(message: A) {
    println("DOWN STREAM", message)
    websocket.foreach { ws =>
      Channels.write(ws.context.getChannel, message)
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
