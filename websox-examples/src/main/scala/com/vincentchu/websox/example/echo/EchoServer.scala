package com.vincentchu.websox.example.echo

import com.vincentchu.websox.websocket._
import com.vincentchu.websox.message.StringMessage
import com.twitter.util.Future

object EchoServer {
  def main(args: Array[String]) {
    val service = new LocalWebsocketService[String] {
      def onConnect(socketId: SocketId): Future[Unit] = {
        println("** onConnect")
        Future.Unit
      }

      def onMessage(socketId: SocketId, msg: String): Future[Unit] = {
        println("** onMessage from %s received: %s".format(socketId, msg))

        if (msg == "closeme") {
          close(socketId)
        } else {
          writeMessage(socketId, msg.toUpperCase)
        }
      }

      def onClose(socketId: SocketId): Future[Unit] = {
        println("** onClose from %s".format(socketId))
        Future.Unit
      }
    }

    val config = ServerConfig(
      StringMessage,
      service,
      8080
    )

    println("** Starting EchoServer")
    Server(config).bind()
  }
}
