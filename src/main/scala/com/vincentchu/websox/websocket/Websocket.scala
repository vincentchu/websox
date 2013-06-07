package com.vincentchu.websox.websocket

import com.twitter.finagle.util.Proc

trait Websocket[A] {
  def socketId: SocketId

  def sendUpstream(mesg: A) {
    upstreamQueue ! mesg
  }

  def sendDownstream(mesg: A) {
    downstreamQueue ! mesg
  }

  def close(): Unit

  def upstreamQueue: Proc[A]
  def downstreamQueue: Proc[A]
}

class WebsocketImpl[A](
  override val socketId: SocketId,
  upstreamCallback: A => Unit,
  downstreamCallback: A => Unit,
  closeCallback: Unit => Unit
) extends Websocket[A] {

  override def upstreamQueue = Proc(upstreamCallback)
  override def downstreamQueue = Proc(downstreamCallback)
  override def close() { closeCallback() }
}

