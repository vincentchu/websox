package com.vincentchu.websox.websocket

import com.twitter.util.Time

object WebSocket {
  def apply(
    socketId: SocketId,
    headers: Map[String, String] = Map.empty,
    connectedAt: Time = Time.now
  ): WebSocket = WebSocketImpl(socketId, headers, connectedAt)
}

trait WebSocket {
  def socketId: SocketId
  def headers: Map[String, String]
  def connectedAt: Time
}

case class WebSocketImpl(
  socketId: SocketId,
  headers: Map[String, String],
  connectedAt: Time
) extends WebSocket
