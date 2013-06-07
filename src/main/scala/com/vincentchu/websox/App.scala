package com.vincentchu.websox

import com.vincentchu.websox.websocket.Server

object App {
  def main(args: Array[String]) {
    println("hello, world")

    Server(8080).bind()
  }
}
