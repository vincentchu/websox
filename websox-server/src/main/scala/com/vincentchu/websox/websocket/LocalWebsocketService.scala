package com.vincentchu.websox.websocket

import com.twitter.util.Future
import java.util.concurrent.ConcurrentHashMap

trait LocalWebsocketService[A] extends WebsocketService[A] {

  import WebsocketService._

  private[this] val socketMap = new ConcurrentHashMap[SocketId, Websocket[A]]()

  protected[this] def get(socketId: SocketId): Option[Websocket[A]] =
    Option(socketMap.get(socketId))

  def registerSocket(ws: Websocket[A]): Future[Unit] = {
    Option(socketMap.putIfAbsent(ws.socketId, ws)) match {
      case Some(_) => Future.exception(SocketIdExists)
      case None    => onConnect(ws.socketId)
    }
  }

  def deregisterSocket(socketId: SocketId, closeChannel: Boolean): Future[Unit] = {
    get(socketId) match {
      case None         => Future.exception(SocketIdNotFound)
      case Some(socket) => onClose(socketId) ensure {
        socketMap.remove(socketId)
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

  def writeMessage(socketIds: Seq[SocketId], mesg: A): Future[Unit] = {
    Future.collect(socketIds map { socketId =>
      get(socketId) match {
        case Some(socket) => Future.value(socket.sendDownstream(mesg))
        case None         => Future.exception(SocketIdNotFound)
      }
    }) map { _ => ()}
  }
}
