package com.zilverline.es2
package behavior

case class EventSourceState(id: Identifier, original: Revision, current: Revision, value: Any)

case class UnitOfWork(
  events: List[UncommittedEvent],
  eventStore: eventstore.EventStore,
  eventSources: Map[Identifier, EventSourceState] = Map.empty
) {
  def trackEventSource(source: Identifier, revision: Revision, value: Any) = {
    require(!eventSources.contains(source), "already tracking " + source)
    copy(eventSources = eventSources + (source -> EventSourceState(source, revision, revision, value)))
  }

  def modifyEventSource[A <: DomainEvent, B](source: Identifier, event: A)(callback: Uncommitted[A] => B) = {
    val originalState = eventSources.getOrElse(source, EventSourceState(source, 0, 0, None))
    val nextRevision = originalState.current + 1
    val uncommitted = Uncommitted(source, nextRevision, event)
    val updatedValue = callback(uncommitted)
    val updatedState = originalState.copy(current = nextRevision, value = updatedValue)
    (copy(uncommitted :: events, eventSources = eventSources.updated(source, updatedState)), updatedValue)
  }
}

trait Reaction[+T] {
  def changes: List[UncommittedEvent]
}

case class Accepted[+T](uow: UnitOfWork, result: T) extends Reaction[T] {
  def changes = uow.events
}

case class Rejected(message: String) extends Reaction[Nothing] {
  def changes = Nil
}

trait Behavior[+A] {
  def apply(uow: UnitOfWork): Reaction[A]

  def map[B](f: A => B) = flatMap(a => accept(f(a)))

  def flatMap[B](next: A => Behavior[B]) = Behavior {uow =>
    this(uow) match {
      case Accepted(uow, result) => next(result)(uow)
      case Rejected(message) => Rejected(message)
    }
  }

  def andThen[B](next: Behavior[B]) = this flatMap (_ => next)
}

object Behavior {
  def apply[T](callback: UnitOfWork => Reaction[T]) = new Behavior[T] {
    def apply(uow: UnitOfWork) = callback(uow)
  }
}
