package com.zilverline.es2

package object behavior {
  def accept[T](result: T) = Behavior(uow => Accepted(uow, result))

  def reject(message: String) = Behavior(_ => Rejected(message))

  def trackEventSource(source: Identifier, revision: Revision, value: Any) = Behavior {
    uow => Accepted(uow.trackEventSource(source, revision, value), ())
  }

  def modifyEventSource[A <: DomainEvent, B](source: Identifier, event: A)(f: Uncommitted[A] => B) = Behavior {
    uow =>
      val (updated, result) = uow.modifyEventSource(source, event)(f)
      Accepted(updated, result)
  }
}
