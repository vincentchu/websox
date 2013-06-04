package com.vincentchu.websox.websocket

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame

import com.twitter.util.Bijection

trait Message[A] extends Bijection[A, TextWebSocketFrame] {





}
