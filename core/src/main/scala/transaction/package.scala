package com.zilverline.es2

package object transaction {
  def pure[T](result: T) = Transaction(uow => TransactionState(uow, result))

  def rollback = Transaction(_ => TransactionState(UnitOfWork(), ()))

  def trackEventSource(source: Identifier, revision: Revision, value: Any) = Transaction {
    uow => TransactionState(uow.trackEventSource(source, revision, value), ())
  }

  def modifyEventSource[A <: DomainEvent, B](source: Identifier, event: A)(f: Uncommitted[A] => B) = Transaction {
    uow =>
      val (updated, result) = uow.modifyEventSource(source, event)(f)
      TransactionState(updated, result)
  }
}
