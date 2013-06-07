package com.vincentchu.websox.oldwebsocket

object Message {
  def fromDecodedMessage[A](socketId: SocketId, message: A) = new Message(
    socketId, message
  )
}

case class Message[+A](socketId: SocketId, message: A)
