package com.vincentchu.websox.websocket

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame

object StringMessageBijection extends MessageBijection[String] {
  def apply(frame: TextWebSocketFrame): String = frame.getText
  def invert(string: String) = new TextWebSocketFrame(string)
}
