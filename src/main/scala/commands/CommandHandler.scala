package com.zilverline.es2
package commands

import behavior._

abstract class CommandHandler[T <: Command, R](implicit m: Manifest[T]) extends (T => Behavior[R]) {
  def commandType: Class[_] = m.erasure
  def invokeWithCommand(command: Command) = apply(command.asInstanceOf[T])
}

object CommandHandler {
  def apply[T <: Command, R](callback: T => Behavior[R])(implicit m: Manifest[T]) = new CommandHandler[T, R] {
    def apply(command: T) = callback(command)
  }
}
