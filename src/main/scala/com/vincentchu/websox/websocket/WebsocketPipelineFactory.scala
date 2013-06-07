package com.vincentchu.websox.websocket

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpResponseEncoder, HttpRequestDecoder}

class WebsocketPipelineFactory extends ChannelPipelineFactory {
  def getPipeline = {
    val pipeline = Channels.pipeline()
    pipeline.addLast("decoder", new HttpRequestDecoder)
    pipeline.addLast("encoder", new HttpResponseEncoder)
    pipeline.addLast("websox", new WebsocketHandler(StringMessageBijection))

    pipeline
  }
}
