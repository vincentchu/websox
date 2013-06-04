package com.vincentchu.websox.codec

import com.twitter.finagle.{Codec, CodecFactory}
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._

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

class WebSocketCodec extends CodecFactory[String, String] {
  def server = Function.const {
    new Codec[String, String] {
      def pipelineFactory = new ChannelPipelineFactory {
        def getPipeline = {
          println("MAKING NEW HANDLER")
          val pipeline = Channels.pipeline()
          pipeline.addLast("decoder", new HttpRequestDecoder)
          pipeline.addLast("encoder", new HttpResponseEncoder)
          pipeline.addLast("foo", new WebSocketHandler)

          pipeline
        }
      }
    }
  }

  def client: this.Client = Function.const {
    throw new UnsupportedOperationException("Clients not supported")
  }
}
