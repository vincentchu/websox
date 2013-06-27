package com.vincentchu.websox.websocket

import com.twitter.util.Future

object WebsocketService {
  object SocketIdNotFound extends Exception
  object SocketIdExists extends Exception
}

trait WebsocketService[A] {
  def registerSocket(ws: Websocket[A]): Future[Unit]
  def deregisterSocket(socketId: SocketId, fireCallback: Boolean): Future[Unit]
  def deregisterSocket(ws: Websocket[A], fireCallback: Boolean): Future[Unit] =
    deregisterSocket(ws.socketId, fireCallback)

  def getSocket(socketId: SocketId): Future[Websocket[A]]

  def isConnected(socketId: SocketId): Future[Boolean]
  def isConnected(ws: Websocket[A]): Future[Boolean] = isConnected(ws.socketId)

  def writeMessage(socketIds: Seq[SocketId], mesg: A): Future[Unit]
  def writeMessage(socketId: SocketId, mesg: A): Future[Unit] = writeMessage(Seq(socketId), mesg)
  def writeMessage(socket: Websocket[A], mesg: A): Future[Unit] = writeMessage(socket.socketId, mesg)
  def close(socketId: SocketId) = deregisterSocket(socketId, fireCallback = true)

  def onConnect(socketId: SocketId): Future[Unit]
  def onMessage(socketId: SocketId, msg: A): Future[Unit]
  def onClose(socketId: SocketId): Future[Unit]
}
