package com.vincentchu.websox.websocket

import com.twitter.finagle.util.Proc
import org.jboss.netty.channel.ChannelHandlerContext

trait Websocket[A] {
  def socketId: SocketId
  def context: ChannelHandlerContext

  def sendUpstream(mesg: A) {
    upstreamQueue ! mesg
  }

  def sendDownstream(mesg: A) {
    println("Websocket: sendDownstream", mesg)
    downstreamQueue ! mesg
  }

  def close(): Unit

  def upstreamQueue: Proc[A]
  def downstreamQueue: Proc[A]
}

class ChannelHandlerContextWebsocket[A](
  val socketId: SocketId,
  val context: ChannelHandlerContext,
  upstreamCallback: A => Unit,
  downstreamCallback: A => Unit,
  closeCallback: () => Unit
) extends Websocket[A] {

  override def upstreamQueue = Proc(upstreamCallback)
  override def downstreamQueue = Proc(downstreamCallback)
  override def close() {
    try {
      closeCallback()
    }

    context.getChannel.close()
  }
}

