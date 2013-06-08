package com.vincentchu.websox

import com.vincentchu.websox.websocket._
import com.twitter.util.Future
import com.vincentchu.websox.message.StringMessage

object App {
  def main(args: Array[String]) {
    println("hello, world")

    val service = new LocalWebsocketService[String] {
      def onConnect(socketId: SocketId): Future[Unit] = {
        println("FooService onConnect")
        Future.Unit
      }

      def onMessage(socketId: SocketId, msg: String): Future[Unit] = {
        println("FooService onMessage received", msg, "from", socketId)
        val mm: String = "You sez: " + msg

        if (msg == "closeme") {
          close(socketId)
        } else {
          writeMessage(socketId, mm)
        }
      }

      def onClose(socketId: SocketId): Future[Unit] = {
        println("FooService onClose from", socketId)
        Future.Unit
      }
    }

    val config = ServerConfig(
      StringMessage,
      service,
      8080
    )

    Server(config).bind()
  }
}
