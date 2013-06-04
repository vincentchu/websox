package com.vincentchu.websox.codec

import com.twitter.finagle.{Codec, CodecFactory}
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import com.vincentchu.websox.websocket.{MessageBijection, WebSocketService, Message, StringMessageBijection}

class FooHandler extends SimpleChannelHandler {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("*** mesgReceived")
    Channels.fireMessageReceived(ctx, "hello there!")
  }
  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("writeRequested")

    e.getMessage match {
      case resp: String =>
        println("*** writeRequested with", resp)
        val httpResp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN)
        val chFut = Channels.future(ctx.getChannel)
        chFut.addListener(ChannelFutureListener.CLOSE)
        Channels.write(ctx, chFut, httpResp)
      case http: HttpResponse if e.getChannel.isOpen =>
        println("*** recv http response")
    }
  }
}

class WebSocketCodec[A](bijection: MessageBijection[A], service: WebSocketService[A]) extends CodecFactory[Message[A], Unit] {
  def server = Function.const {
    new Codec[Message[A], Unit] {
      def pipelineFactory = new ChannelPipelineFactory {
        def getPipeline = {
          println("MAKING NEW HANDLER")
          val pipeline = Channels.pipeline()
          pipeline.addLast("decoder", new HttpRequestDecoder)
          pipeline.addLast("encoder", new HttpResponseEncoder)
          pipeline.addLast("foo", new WebSocketHandler(bijection, service))

          pipeline
        }
      }
    }
  }

  def client: this.Client = Function.const {
    throw new UnsupportedOperationException("Clients not supported")
  }
}
