package com.zilverline.es2
package behavior

case class EventSourceState(id: Identifier, original: Revision, current: Revision, value: Any)

case class UnitOfWork(
  events: List[UncommittedEvent],
  eventSources: Map[Identifier, EventSourceState] = Map.empty) {

  def getEventSource(source: Identifier): Option[Any] = eventSources.get(source).map(_.value)

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

trait Reaction[+Error, +Result] {
  def changes: List[UncommittedEvent]
}

case class Accepted[+Result](uow: UnitOfWork, result: Result) extends Reaction[Nothing, Result] {
  def changes = uow.events
}

case class Rejected[+Error](message: Error) extends Reaction[Error, Nothing] {
  def changes = Nil
}

trait Behavior[+Error, +Result] {
  def apply(uow: UnitOfWork): Reaction[Error, Result]

  def map[B](f: Result => B) = flatMap(a => accept(f(a)))

  def flatMap[E1 >: Error, B](next: Result => Behavior[E1, B]) = Behavior {uow =>
    this(uow) match {
      case Accepted(uow, result) => next(result)(uow)
      case Rejected(error) => Rejected(error)
    }
  }

  def andThen[E1 >: Error, B](next: Behavior[E1, B]) = this flatMap (_ => next)

  def trigger = apply(UnitOfWork(Nil))

  def result = trigger match {
    case Accepted(_, result) => result
  }
}

object Behavior {
  def apply[Error, Result](callback: UnitOfWork => Reaction[Error, Result]) = new Behavior[Error, Result] {
    def apply(uow: UnitOfWork) = callback(uow)
  }
}
