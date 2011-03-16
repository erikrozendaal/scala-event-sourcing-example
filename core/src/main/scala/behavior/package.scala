package com.zilverline.es2

package object behavior {
  def pure[T](result: T) = Behavior(uow => Reaction(uow, result))

  def rollback = Behavior(_ => Reaction(TrackedEventSources(), ()))

  def trackEventSource(eventSourceId: Identifier, revision: Revision, value: Any) = Behavior {
    uow => Reaction(uow.trackEventSource(eventSourceId, revision, value), ())
  }

  def modifyEventSource[A <: DomainEvent, B](source: Identifier, event: A)(f: Uncommitted[A] => B) = Behavior {
    uow =>
      val (updated, result) = uow.modifyEventSource(source, event)(f)
      Reaction(updated, result)
  }
}
