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
 * This is a simple chat application. When each client connects, its socketId
 * and when it connected are recorded in a ConcurrentHashMap. When a message
 * is received from a connected client, the server will broadcast to all other
 * connected clients, except the client that originally sent the message.
 *
 * When a connected client disconnects, this is broadcasted to the other
 * connected clients and the leaving client's socketId is removed from the
 * ConcurrentHashMap.
 */
object ChatServer {
  def main(args: Array[String]) {

    val service = new LocalWebsocketService[String] {

      // This keeps track of connected clients
      private[this] val connectedClients = new ConcurrentHashMap[SocketId, Time]()

      def onConnect(socketId: SocketId): Future[Unit] = {
        println("** connection received from %s".format(socketId))
        connectedClients.put(socketId, Time.now) // Record when client connected
        Future.Unit
      }

      def onMessage(socketId: SocketId, msg: String): Future[Unit] = {
        println("** received message from %s: %s".format(socketId, msg))
        val chatMesg = "%s said: %s".format(socketId, msg)
        broadcast(socketId, chatMesg)
      }

      def onClose(socketId: SocketId): Future[Unit] = {
        println("** onClose from %s".format(socketId))

        val time = Option(connectedClients.remove(socketId)) map { _.toString }
        val mesg = "%s has left the chat".format(socketId, time.getOrElse(""))
        broadcast(socketId, mesg)
      }

      private[this] def broadcast(socketId: SocketId, mesg: String): Future[Unit] = {
        val chatFutures = connectedClients.keySet.asScala.toSeq flatMap {
          case clientId if clientId == socketId => None // Don't broadcast to self
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
