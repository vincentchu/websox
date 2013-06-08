package com.vincentchu.websox.websocket

import java.util.concurrent.ConcurrentHashMap
import com.twitter.util.Future

trait LocalWebsocketService[A] extends WebsocketService[A] {

  import WebsocketService._

  private[this] val contextMap = new ConcurrentHashMap[SocketId, Websocket[A]]()

  protected[this] def get(socketId: SocketId): Option[Websocket[A]] =
    Option(contextMap.get(socketId))

  def registerSocket(ws: Websocket[A]): Future[Unit] = {
    Option(contextMap.putIfAbsent(ws.socketId, ws)) match {
      case Some(_) => Future.exception(SocketIdExists)
      case None    => Future.Unit
    }
  }

  def deregisterSocket(socketId: SocketId, closeChannel: Boolean): Future[Unit] = {
    get(socketId) match {
      case None         => Future.exception(SocketIdNotFound)
      case Some(socket) => onClose(socketId) ensure {
        contextMap.remove(socketId)
        if (closeChannel) {
          socket.close()
        }
      }
    }
  }

  def getSocket(socketId: SocketId): Future[Websocket[A]] = {
    get(socketId) match {
      case Some(socket) => Future.value(socket)
      case None         => Future.exception(SocketIdNotFound)
    }
  }

  def isConnected(socketId: SocketId): Future[Boolean] =
    Future.value(get(socketId).isDefined)

  def writeMessage(socketId: SocketId, mesg: A): Future[Unit] = {
    get(socketId) match {
      case Some(socket) => Future.value(socket.sendDownstream(mesg))
      case None         => Future.exception(SocketIdNotFound)
    }
  }
}
