package com.vincentchu.websox.websocket

import com.vincentchu.websox.message.Message
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpResponseEncoder, HttpRequestDecoder}

class WebsocketPipelineFactory[A](mesg: Message[A], service: WebsocketService[A]) extends ChannelPipelineFactory {
  def getPipeline = {
    val pipeline = Channels.pipeline()
    pipeline.addLast("decoder", new HttpRequestDecoder)
    pipeline.addLast("encoder", new HttpResponseEncoder)
    pipeline.addLast("websox", new WebsocketHandler(mesg.converter, service))

    pipeline
  }
}
