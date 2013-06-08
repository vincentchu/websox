package com.vincentchu.websox.example.echo

import com.vincentchu.websox.websocket._
import com.vincentchu.websox.message.StringMessage
import com.twitter.util.Future

/**
 * EchoServer
 *
 * This is a simple, modified echo server websocket app. When it receives a
 * message from a connected client, it echos the message back to the client,
 * except the message will be upper cased. If the server receives "closeme"
 * from the connected client, it will close the websocket from the server
 * side.
 */
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
          // Initiate socket close from the server side
          close(socketId)
        } else {
          // Write message to connected client
          writeMessage(socketId, msg.toUpperCase)
        }
      }

      def onClose(socketId: SocketId): Future[Unit] = {
        println("** onClose from %s".format(socketId))
        Future.Unit
      }
    }

    val config = ServerConfig(
      StringMessage, // Type of message your server handles
      service,       // Your websocket service with application logic
      8080           // The port you wish to bind to
    )

    println("** Starting EchoServer")
    Server(config).bind() // .bind() starts the server and binds port
  }
}
