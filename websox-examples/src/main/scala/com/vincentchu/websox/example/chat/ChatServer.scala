package com.vincentchu.websox.example.chat

import com.vincentchu.websox.websocket._
import com.vincentchu.websox.message.StringMessage
import com.twitter.util.Future
import java.util.concurrent.ConcurrentHashMap
import com.twitter.util.Time
import scala.collection.JavaConverters._

/**
 * ChatServer
 *
 * This is a simple, modified echo server websocket app. When it receives a
 * message from a connected client, it echos the message back to the client,
 * except the message will be upper cased. If the server receives "closeme"
 * from the connected client, it will close the websocket from the server
 * side.
 */
object ChatServer {
  def main(args: Array[String]) {

    val service = new LocalWebsocketService[String] {

      private[this] val connectedClients = new ConcurrentHashMap[SocketId, Time]()

      def onConnect(socketId: SocketId): Future[Unit] = {
        println("** connection received from %s".format(socketId))
        connectedClients.put(socketId, Time.now)
        Future.Unit
      }

      def onMessage(socketId: SocketId, msg: String): Future[Unit] = {
        println("** received message from %s: %s".format(socketId, msg))
        val chatMesg = "%s said: %s".format(socketId, msg)
        broadcast(socketId, chatMesg)
      }

      def onClose(socketId: SocketId): Future[Unit] = {
        println("** onClose from %s".format(socketId))
        Future.Unit
      }

      private[this] def broadcast(socketId: SocketId, mesg: String): Future[Unit] = {
        val chatFutures = connectedClients.keySet.asScala.toSeq flatMap {
          case clientId if clientId == socketId => None
          case clientId => Some(writeMessage(clientId, mesg))
        }

        Future.collect(chatFutures) map { _ =>
          ()
        }
      }
    }

    val config = ServerConfig(
      StringMessage, // Type of message your server handles
      service,       // Your websocket service with application logic
      8080           // The port you wish to bind to
    )

    println("** Starting ChatServer")
    Server(config).bind() // .bind() starts the server and binds port
  }
}
