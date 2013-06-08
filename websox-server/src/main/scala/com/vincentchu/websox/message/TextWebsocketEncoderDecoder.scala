package com.vincentchu.websox.message

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.twitter.util.Bijection

object TextWebsocketEncoderDecoder extends Bijection[String, TextWebSocketFrame] {
  def apply(string: String) = new TextWebSocketFrame(string)
  def invert(frame: TextWebSocketFrame): String = frame.getText
}
