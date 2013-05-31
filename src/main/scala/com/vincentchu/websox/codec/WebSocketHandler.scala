package com.vincentchu.websox.codec

import com.twitter.finagle.netty3.Conversions._
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.websocketx._
import scala.Some

class WebSocketHandler extends SimpleChannelHandler {

  private var handshakerFactory: Option[WebSocketServerHandshakerFactory] = None
  private var handshaker: Option[WebSocketServerHandshaker] = None

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("** writeRequested")
    e.getMessage match {
      case _ => ctx.sendDownstream(e)
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case httpReq: HttpRequest    => handleHandshake(ctx, httpReq)
      case wsFrame: WebSocketFrame => handleWebSocketReq(ctx, wsFrame)
      case _                       => ctx.getChannel.close()
    }
  }

  private[this] def handleWebSocketReq(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    println("handle wsSocketRequest")
    frame match {
      case textFrame: TextWebSocketFrame =>
        val txt = textFrame.getText
        val resp = new TextWebSocketFrame(txt.toUpperCase)
        ctx.getChannel.write(resp)

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
