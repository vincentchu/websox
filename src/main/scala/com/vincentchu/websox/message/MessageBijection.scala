package com.vincentchu.websox.message

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame

import com.twitter.util.Bijection

trait MessageBijection[A] extends Bijection[TextWebSocketFrame, A]
