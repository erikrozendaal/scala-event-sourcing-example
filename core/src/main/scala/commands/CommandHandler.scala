package com.zilverline.es2
package commands

import behavior._

abstract class CommandHandler[T <: Command, E, R](implicit m: Manifest[T]) {
  def apply(command: T): Behavior[E, R]
  def commandType: Class[_] = m.erasure
  def invokeWithCommand(command: Command) = apply(command.asInstanceOf[T])
}

object CommandHandler {
  def apply[T <: Command, E, R](callback: T => Behavior[E, R])(implicit m: Manifest[T]) = new CommandHandler[T, E, R] {
    def apply(command: T) = callback(command)
  }

  def updateCommandHandler = CommandHandler {
    command: Update =>
      trackEventSource(command.source, command.revision, ())
        .andThen(modifyEventSource(command.source, command.event)(_ => ()))
  }
}
