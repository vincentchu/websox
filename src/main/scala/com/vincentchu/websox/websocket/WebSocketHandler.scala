package com.vincentchu.websox.websocket

import com.twitter.finagle.netty3.Conversions._
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.websocketx._
import java.util.UUID
import com.twitter.util.Promise

class WebsocketHandler[A](
  mesgConverter: MessageBijection[A],
  service: WebsocketService[A]
) extends SimpleChannelHandler {

  private[this] val socketId: SocketId = UUID.randomUUID.toString
  private[this] val websocket = new Promise[Websocket[A]]
  private[this] var handshakerFactory: Option[WebSocketServerHandshakerFactory] = None
  private[this] var handshaker: Option[WebSocketServerHandshaker] = None

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case _: HttpResponse | _: TextWebSocketFrame | _: CloseWebSocketFrame =>
        ctx.sendDownstream(e)
      case resp: A =>
        ctx.getChannel.write(mesgConverter.invert(resp))
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case httpReq: HttpRequest    => handleHandshake(ctx, httpReq)
      case wsFrame: WebSocketFrame => handleWebSocketReq(ctx, wsFrame)
      case message: A              => websocket foreach { _.sendUpstream(message) }
    }
  }

  private[this] def handleWebSocketReq(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    frame match {
      case textFrame: TextWebSocketFrame =>
        Channels.fireMessageReceived(ctx.getChannel, mesgConverter(textFrame))

      case pingFrame: PingWebSocketFrame =>
        ctx.getChannel.write(new PongWebSocketFrame(frame.getBinaryData))

      case closeFrame: CloseWebSocketFrame =>
        websocket foreach { ws =>
          service.deregisterSocket(ws, fireCallback = false)
        } ensure {
          handshaker.foreach { _.close(ctx.getChannel, closeFrame) }
        }
    }
  }

  private[this] def handleHandshake(ctx: ChannelHandlerContext, req: HttpRequest) {
    setHandshakerFactory(req)
    handshaker match {
      case Some(wsHandshaker) =>
        try {
          wsHandshaker.handshake(ctx.getChannel, req).toTwitterFuture map { _ =>
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
    websocket map { ws =>
      val closeFrame = new CloseWebSocketFrame(1000, "Server requested close")
      Channels.write(ws.context.getChannel, closeFrame)
    }
  }

  private[this] def downstreamCallback(message: A) {
    websocket.foreach { ws =>
      Channels.write(ws.context.getChannel, message)
    }
  }

  private[this] def closeChannel(ctx: ChannelHandlerContext) {
    ctx.getChannel.close()
  }

  private[this] def sendErrorAndClose(ctx: ChannelHandlerContext) {
    handshakerFactory foreach { factory =>
      factory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel)
      closeChannel(ctx)
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
