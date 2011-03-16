package com.zilverline.es2

package object transaction {
  def pure[T](result: T) = Transaction(uow => TransactionState(uow, result))

  def rollback = Transaction(_ => TransactionState(TrackedEventSources(), ()))

  def trackEventSource(eventSourceId: Identifier, revision: Revision, value: Any) = Transaction {
    uow => TransactionState(uow.trackEventSource(eventSourceId, revision, value), ())
  }

  def modifyEventSource[A <: DomainEvent, B](source: Identifier, event: A)(f: Uncommitted[A] => B) = Transaction {
    uow =>
      val (updated, result) = uow.modifyEventSource(source, event)(f)
      TransactionState(updated, result)
  }
}
