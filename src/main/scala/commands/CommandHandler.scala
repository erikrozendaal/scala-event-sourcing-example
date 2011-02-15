package com.zilverline.es2
package commands

import events._

case class UnitOfWork(aggregate: Option[Identifier], events: Option[Event])

object UnitOfWork {
  def empty = UnitOfWork(None, None)
}

abstract class CommandHandler[T <: Command](implicit m: Manifest[T]) extends (T => Transaction) {
  def commandType: Class[_] = m.erasure
  def invokeWithCommand(command: Command) = apply(command.asInstanceOf[T])
}

object CommandHandler {

  def transaction(callback: UnitOfWork => UnitOfWork) = callback

  def handler[T <: Command](callback: T => Transaction)(implicit m: Manifest[T]) = new CommandHandler[T] {
    def apply(command: T) = callback(command)
  }

  def save(aggregate: Identifier, event: Event) = transaction { uow =>
    uow.copy(Some(aggregate), Some(event))
  }

  def accept = transaction(identity)

  def reject = transaction(_ => UnitOfWork(None, None))
}
