package com.zilverline.es2
package commands

import behavior._

abstract class CommandHandler[T <: Command, R](implicit m: Manifest[T]) {
  def apply(command: T): Behavior[R]
  def commandType: Class[_] = m.erasure
  def invokeWithCommand(command: Command) = apply(command.asInstanceOf[T])
}

object CommandHandler {
  def apply[T <: Command] = new {
    def apply[R](callback: T => Behavior[R])(implicit m: Manifest[T]) = new CommandHandler[T, R] {
      def apply(command: T) = callback(command)
    }
  }

  def updateCommandHandler = CommandHandler[Update] {command =>
    Behavior.trackEventSource(command.eventSourceId, command.expectedRevision, ()).flatMap(_ =>
    Behavior.record(command.eventSourceId, command.event)(_ => ()))
  }
}
