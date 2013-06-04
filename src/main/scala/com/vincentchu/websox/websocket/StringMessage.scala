package com.vincentchu.websox.websocket

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame

object StringMessage extends Message[String] {
  def apply(string: String) = new TextWebSocketFrame(string)
  def invert(frame: TextWebSocketFrame): String = frame.getText
}
