package com.zilverline.es2
package commands

import events._

sealed trait UnitOfWork
case object Empty extends UnitOfWork
case object Rejected extends UnitOfWork
case class Accepted(aggregate: Identifier, event: Event) extends UnitOfWork

object UnitOfWork {
  def empty = Empty
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

  def save(aggregate: Identifier, event: Event) = transaction {
    case Empty => Accepted(aggregate, event)
    case Accepted(_, _) => error("cannot change multiple aggregates in single transaction")
    case Rejected => Rejected
  }

  def accept = transaction(identity)

  def reject = transaction(_ => Rejected)
}
