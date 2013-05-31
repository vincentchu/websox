package com.vincentchu.websox.codec

import com.twitter.finagle.netty3.Conversions._
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.websocketx.{WebSocketServerHandshaker, WebSocketServerHandshakerFactory}

class WebSocketHandler extends SimpleChannelHandler {

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("** writeRequested")
    e.getMessage match {
      case _: HttpResponse =>
        println("RETURN")
        ctx.sendDownstream(e)
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case httpReq: HttpRequest => handleHandshake(ctx, httpReq)
      case _ =>
        println("HERE")
    }
  }

  private[this] def handleHandshake(ctx: ChannelHandlerContext, req: HttpRequest) {
    val factory = wsHandshakerFactory(req)
    Option(factory.newHandshaker(req)) match {
      case Some(wsHandshaker) =>
        println("ATTEMPT HANDSHAKE")
        try {
          wsHandshaker.handshake(ctx.getChannel, req).toTwitterFuture map { _ =>
            println("** handshake completed!")
          }
        } catch {
          case _: Exception => sendErrorAndClose(factory, ctx)
        }

      case _ => sendErrorAndClose(factory, ctx)
    }
  }

  private[this] def sendErrorAndClose(
    factory: WebSocketServerHandshakerFactory,
    ctx: ChannelHandlerContext
  ) {
    println("SEENDING ERR")
    factory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel)
    ctx.getChannel.close()
  }

  private[this] def isWebSocketRequest(req: HttpRequest) =
    Option(req.getHeader(HttpHeaders.Names.SEC_WEBSOCKET_PROTOCOL)).isDefined

  private[this] def wsHandshakerFactory(req: HttpRequest): WebSocketServerHandshakerFactory = {
    val location = "ws://" + req.getHeader(HttpHeaders.Names.HOST) + "/"
    new WebSocketServerHandshakerFactory(location, null, false)
  }
}
