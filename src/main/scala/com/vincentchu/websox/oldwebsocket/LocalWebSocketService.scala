package com.vincentchu.websox.oldwebsocket

import java.util.concurrent.ConcurrentHashMap
import org.jboss.netty.channel.ChannelHandlerContext
import com.twitter.util.Future
import com.twitter.finagle.netty3.Conversions._

trait LocalWebSocketService[A] extends WebSocketService[A] {

  import WebSocketService._

  private[this] case class WebSocketContext(
    websocket: WebSocket,
    handlerContext: ChannelHandlerContext
  ) {
    def getChannel = handlerContext.getChannel
    def write(mesg: A): Future[Unit] = {
      getChannel.write(Message(websocket.socketId, mesg)).toTwitterFuture
    }

    def close(): Future[Unit] = getChannel.close().toTwitterFuture
  }

  private[this] val contextMap = new ConcurrentHashMap[SocketId, WebSocketContext]()

  private[this] def getSocketContext(socketId: SocketId): Option[WebSocketContext] =
    Option(contextMap.get(socketId))

  def registerSocket(socketId: SocketId, headers: Map[String, String], ctx: ChannelHandlerContext): Future[WebSocket] = {
    val ws = WebSocket(socketId, headers)
    val wsContext = WebSocketContext(ws, ctx)

    Option(contextMap.putIfAbsent(socketId, wsContext)) match {
      case Some(_) => Future.exception(SocketIdExists)
      case None    => Future.value(ws)
    }
  }

  def getSocket(socketId: SocketId): Future[WebSocket] = {
    getSocketContext(socketId) match {
      case Some(context) => Future.value(context.websocket)
      case None          => Future.exception(SocketIdNotFound)
    }
  }

  def isConnected(socketId: SocketId): Future[Boolean] =
    Future.value(getSocketContext(socketId).isDefined)

  def writeMessage(socketId: SocketId, mesg: A): Future[Unit] = {
    getSocketContext(socketId) match {
      case Some(context) => context.write(mesg)
      case None          => Future.exception(SocketIdNotFound)
    }
  }

  def closeSocket(socketId: SocketId): Future[Unit] = {
    getSocketContext(socketId) match {
      case Some(context) => context.close()
      case None          => Future.exception(SocketIdNotFound)
    }
  }
}
