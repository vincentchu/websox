package com.vincentchu.websox.websocket

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpResponseEncoder, HttpRequestDecoder}
import com.twitter.util.Future

class FooService extends LocalWebsocketService[String] {
  def onConnect(socketId: SocketId): Future[Unit] = {
    println("FooService onConnect")
    Future.Unit
  }

  def onMessage(socketId: SocketId, msg: String): Future[Unit] = {
    println("FooService onMessage received", msg, "from", socketId)
    val mm: String = "You sez: " + msg

    writeMessage(socketId, mm)
  }

  def onClose(socketId: SocketId): Future[Unit] = {
    println("FooService onClose from", socketId)
    Future.Unit
  }
}

class WebsocketPipelineFactory extends ChannelPipelineFactory {
  def getPipeline = {
    val pipeline = Channels.pipeline()
    pipeline.addLast("decoder", new HttpRequestDecoder)
    pipeline.addLast("encoder", new HttpResponseEncoder)
    pipeline.addLast("websox", new WebsocketHandler[String](StringMessageBijection, new FooService))

    pipeline
  }
}
