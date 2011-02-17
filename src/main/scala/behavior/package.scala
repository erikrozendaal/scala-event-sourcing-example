package com.zilverline.es2

package object behavior {
  def accept[T](result: T) = Behavior(uow => Accepted(uow, result))

  def reject(message: String) = Behavior(_ => Rejected(message))

  def record[A <: DomainEvent](source: Identifier, event: A) = Behavior {
    uow =>
      val uncommitted = Uncommitted(source, event)
      Accepted(uow.copy(uncommitted :: uow.events), uncommitted)
  }

  def guard(condition: Boolean, message: => String) =
    if (condition) accept() else reject(message)
}
