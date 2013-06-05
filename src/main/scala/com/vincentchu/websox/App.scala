package com.vincentchu.websox

import com.twitter.util.Future
import com.twitter.finagle.Service
import com.vincentchu.websox.codec._
import java.net.InetSocketAddress
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.vincentchu.websox.websocket.{StringMessageBijection, WebSocket, LocalWebSocketService}
import com.vincentchu.websox.test.TestCodec

object App {
  def main(args: Array[String]) {
    println("hello, world")

//    val service = new Service[String, Unit] {
//      def apply(req: String): Future[Unit] = {
//        println("RECV:", req)
//        Future.Unit
//      }
//    }
//
//    val server: Server = ServerBuilder()
//      .codec(new TestCodec)
//      .bindTo(new InetSocketAddress(8080))
//      .name("websox")
//      .build(service)


    val service = new LocalWebSocketService[String] {
      def onConnect(ws: WebSocket) = {
        println("ONCONNECT!")
        Future.Unit
      }

      def onMessage(ws: WebSocket, msg: String) = {
        println("LocalWebSocketService got", msg)

        val m = "ZOMG you sent: " + msg
//        writeMessage(ws, m)
        Future.value(m)
      }

      def onClose(ws: WebSocket) = {
        println("ONCLOSE")
        Future.Unit
      }
    }

    val server: Server = ServerBuilder()
      .codec(new WebSocketCodec(StringMessageBijection))
      .bindTo(new InetSocketAddress(8080))
      .name("websox")
      .build(service)
  }
}
