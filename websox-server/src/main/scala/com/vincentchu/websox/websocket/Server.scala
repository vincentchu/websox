package com.vincentchu.websox.websocket

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress

object Server {
  def apply[A](config: ServerConfig[A]) = new Server(config)
}

class Server[A](val config: ServerConfig[A]) {

  private[this] val factory = new NioServerSocketChannelFactory(
    Executors.newCachedThreadPool,
    Executors.newCachedThreadPool
  )

  private[this] val bootstrap = new ServerBootstrap(factory)

  bootstrap.setPipelineFactory(new WebsocketPipelineFactory(config.converter, config.service))
  bootstrap.setOption("child.tcpNoDelay", true)
  bootstrap.setOption("child.keepAlive", true)

  def bind() = bootstrap.bind(new InetSocketAddress(config.port))
}
