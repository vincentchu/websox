package com.vincentchu.websox.oldwebsocket

import com.twitter.util.Future
import org.jboss.netty.channel.ChannelHandlerContext
import com.twitter.finagle.Service

object WebSocketService {
  object SocketIdNotFound extends Exception
  object SocketIdExists extends Exception
}

trait WebSocketService[-A] extends Service[Message[A], String] {
  def registerSocket(
    socketId: SocketId,
    headers: Map[String, String],
    ctx: ChannelHandlerContext
  ): Future[WebSocket]

  def apply(req: Message[A]): Future[String] = {
//    getSocket(req.socketId) flatMap { ws =>
//      onMessage(ws, req.message)
//    }

    val ws = WebSocket("socketId")
    onMessage(ws, req.message) map { _ => "asdf"}
  }

  def getSocket(socketId: SocketId): Future[WebSocket]

  def isConnected(socketId: SocketId): Future[Boolean]
  def isConnected(ws: WebSocket): Future[Boolean] = isConnected(ws.socketId)

  def writeMessage(socketId: SocketId, mesg: A): Future[Unit]
  def writeMessage(ws: WebSocket, mesg: A): Future[Unit] = writeMessage(ws.socketId, mesg)

  def closeSocket(socketId: SocketId): Future[Unit]
  def closeSocket(ws: WebSocket): Future[Unit] = closeSocket(ws.socketId)

  def onConnect(ws: WebSocket): Future[Unit]
  def onMessage(ws: WebSocket, msg: A): Future[Unit]
  def onClose(ws: WebSocket): Future[Unit]
}
