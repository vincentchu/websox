package com.vincentchu.websox.websocket

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress

object Server {
  def apply(port: Int) = new Server(port)
}

class Server(val port: Int) {

  private[this] val factory = new NioServerSocketChannelFactory(
    Executors.newCachedThreadPool,
    Executors.newCachedThreadPool
  )

  private[this] val bootstrap = new ServerBootstrap(factory)

  bootstrap.setPipelineFactory(new WebsocketPipelineFactory)
  bootstrap.setOption("child.tcpNoDelay", true)
  bootstrap.setOption("child.keepAlive", true)

  def bind() = bootstrap.bind(new InetSocketAddress(port))
}
