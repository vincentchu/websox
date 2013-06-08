package com.vincentchu.websox.websocket

import com.vincentchu.websox.message.Message

case class ServerConfig[A](
  converter: Message[A],
  service: WebsocketService[A],
  port: Int = 8080
)
