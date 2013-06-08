package com.vincentchu.websox.websocket

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpResponseEncoder, HttpRequestDecoder}
import com.twitter.util.Future
import com.vincentchu.websox.message.{Message, StringMessageBijection}


class WebsocketPipelineFactory[A](mesg: Message[A], service: WebsocketService[A]) extends ChannelPipelineFactory {
  def getPipeline = {
    val pipeline = Channels.pipeline()
    pipeline.addLast("decoder", new HttpRequestDecoder)
    pipeline.addLast("encoder", new HttpResponseEncoder)
    pipeline.addLast("websox", new WebsocketHandler(mesg.encoderDecoder, service))

    pipeline
  }
}
