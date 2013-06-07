package com.vincentchu.websox.websocket

import com.twitter.util.Future
import org.jboss.netty.channel.ChannelHandlerContext
import com.twitter.finagle.Service
import com.vincentchu.websox.websocket.Message

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

  def writeMessage(socketId: SocketId, mesg: A): Future[Unit]
  def writeMessage(ws: Websocket[A], mesg: A): Future[Unit] = writeMessage(ws.socketId, mesg)
  def close(socketId: SocketId) = deregisterSocket(socketId, fireCallback = true)

  def onConnect(socketId: SocketId): Future[Unit]
  def onMessage(socketId: SocketId, msg: A): Future[Unit]
  def onClose(socketId: SocketId): Future[Unit]
}
