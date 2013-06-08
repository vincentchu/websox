package com.vincentchu.websox.message

object StringMessage extends Message[String] {
  def apply(string: String)  = string
  def invert(string: String) = string
}
