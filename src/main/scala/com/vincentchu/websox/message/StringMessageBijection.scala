package com.vincentchu.websox.message

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.vincentchu.websox.message.MessageBijection

object StringMessageBijection  extends MessageBijection[String] {
  def apply(frame: TextWebSocketFrame): String = frame.getText
  def invert(string: String) = new TextWebSocketFrame(string)
}
