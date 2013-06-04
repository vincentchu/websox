package com.vincentchu.websox

import com.twitter.util.Future
import com.twitter.finagle.Service
import com.vincentchu.websox.codec._
import java.net.InetSocketAddress
import com.twitter.finagle.builder.{Server, ServerBuilder}

object App {
  def main(args: Array[String]) {
    println("hello, world")

    val service = new Service[String, Unit] {
      def apply(req: String): Future[Unit] = {
        println("RECV:", req)
//        Future.value("HELLO")
        Future.Unit
      }
    }

    val server: Server = ServerBuilder()
      .codec(new WebSocketCodec)
      .bindTo(new InetSocketAddress(8080))
      .name("websox")
      .build(service)
  }
}
