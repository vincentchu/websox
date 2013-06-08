package com.vincentchu.websox.message

import com.twitter.util.Bijection

trait Message[A] extends Bijection[A, String] {
  def encoderDecoder = this andThen(TextWebsocketEncoderDecoder)
}
