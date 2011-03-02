package com.zilverline.es2
package commands

import transaction._

abstract class CommandHandler[T <: Command, R](implicit m: Manifest[T]) {
  def apply(command: T): Transaction[R]
  def commandType: Class[_] = m.erasure
  def invokeWithCommand(command: Command) = apply(command.asInstanceOf[T])
}

object CommandHandler {
  def apply[T <: Command, R](callback: T => Transaction[R])(implicit m: Manifest[T]) = new CommandHandler[T, R] {
    def apply(command: T) = callback(command)
  }

  def updateCommandHandler = CommandHandler {
    command: Update =>
      trackEventSource(command.source, command.revision, ())
        .andThen(modifyEventSource(command.source, command.event)(_ => ()))
  }
}
