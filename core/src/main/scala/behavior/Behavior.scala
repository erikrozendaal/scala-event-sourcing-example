package com.zilverline.es2
package behavior

case class EventSourceState[+A](id: Identifier, original: Revision, current: Revision, value: A, changes: IndexedSeq[DomainEvent]) {
  def modify[B, E <: DomainEvent](event: E)(callback: Uncommitted[E] => B): EventSourceState[B] = {
    val nextRevision = current + 1
    val updatedValue = callback(Uncommitted(id, nextRevision, event))
    copy(current = nextRevision, value = updatedValue, changes = changes :+ event)
  }
}

case class UnitOfWork(
  eventSources: Map[Identifier, EventSourceState[Any]] = Map.empty) {

  def getEventSource(source: Identifier): Option[Any] = eventSources.get(source).map(_.value)

  def trackEventSource(source: Identifier, revision: Revision, value: Any) = {
    require(!eventSources.contains(source), "already tracking " + source)
    copy(eventSources = eventSources + (source -> EventSourceState(source, revision, revision, value, IndexedSeq.empty)))
  }

  def modifyEventSource[A <: DomainEvent, B](source: Identifier, event: A)(callback: Uncommitted[A] => B) = {
    val originalState = eventSources.getOrElse(source, EventSourceState(source, 0, 0, None, IndexedSeq.empty))
    val updatedState = originalState.modify(event)(callback)
    (copy(eventSources.updated(source, updatedState)), updatedState.value)
  }
}

sealed trait Reaction[+Error, +Result] {
  def changes(source: Identifier): Seq[DomainEvent]
}

case class Accepted[+Result](uow: UnitOfWork, result: Result) extends Reaction[Nothing, Result] {
  def changes(source: Identifier) = uow.eventSources.get(source).map(_.changes).getOrElse(Seq.empty)
}

case class Rejected[+Error](message: Error) extends Reaction[Error, Nothing] {
  def changes(source: Identifier) = Nil
}

trait Behavior[+Error, +Result] {
  def apply(uow: UnitOfWork): Reaction[Error, Result]

  def map[B](f: Result => B) = flatMap(a => accept(f(a)))

  def flatMap[E >: Error, B](next: Result => Behavior[E, B]) = Behavior {uow =>
    this(uow) match {
      case Accepted(uow, result) => next(result)(uow)
      case Rejected(error) => Rejected(error)
    }
  }

  def andThen[E >: Error, B](next: Behavior[E, B]) = this flatMap (_ => next)

  def trigger = apply(UnitOfWork())

  def result = (trigger: @unchecked) match {
    case Accepted(_, result) => result
  }
}

object Behavior {
  def apply[Error, Result](callback: UnitOfWork => Reaction[Error, Result]) = new Behavior[Error, Result] {
    def apply(uow: UnitOfWork) = callback(uow)
  }
}
