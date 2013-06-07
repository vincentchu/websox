package com.vincentchu.websox.websocket

object Message {
  def fromDecodedMessage[A](socketId: SocketId, message: A) = new Message(
    socketId, message
  )
}

case class Message[+A](socketId: SocketId, message: A)
