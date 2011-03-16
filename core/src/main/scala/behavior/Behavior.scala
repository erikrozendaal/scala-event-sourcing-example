package com.zilverline.es2
package behavior

private[behavior] case class TrackedEventSource[+A](
  id: Identifier,
  originalRevision: Revision = InitialRevision,
  changes: IndexedSeq[DomainEvent] = IndexedSeq.empty,
  currentValue: Option[A] = None
  ) {
  def currentRevision = originalRevision + changes.size

  def modify[E <: DomainEvent, B](event: E)(callback: Uncommitted[E] => B): TrackedEventSource[B] = {
    val updatedValue = callback(Uncommitted(id, currentRevision + 1, event))
    copy(changes = changes :+ event, currentValue = Some(updatedValue))
  }
}

private[behavior] case class TrackedEventSources(eventSources: Map[Identifier, TrackedEventSource[Any]] = Map.empty) {
  def getEventSource(eventSourceId: Identifier): Option[Any] = eventSources.get(eventSourceId).flatMap(_.currentValue)

  def trackEventSource(eventSourceId: Identifier, originalRevision: Revision, value: Any) = {
    require(!eventSources.contains(eventSourceId), "already tracking " + eventSourceId)
    copy(eventSources = eventSources + (eventSourceId -> TrackedEventSource(eventSourceId, originalRevision, IndexedSeq.empty, Some(value))))
  }

  def modifyEventSource[A <: DomainEvent, B](eventSourceId: Identifier, event: A)(callback: Uncommitted[A] => B) = {
    val originalState = eventSources.getOrElse(eventSourceId, TrackedEventSource(eventSourceId))
    val updatedState = originalState.modify(event)(callback)
    (copy(eventSources.updated(eventSourceId, updatedState)), updatedState.currentValue.get)
  }
}

case class Reaction[+A](tracked: TrackedEventSources, result: A)

trait Behavior[+A] {
  def apply(tracked: TrackedEventSources): Reaction[A]

  def map[B](f: A => B) = flatMap(a => pure(f(a)))

  def flatMap[B](f: A => Behavior[B]) = Behavior {tracked =>
    this(tracked) match {
      case Reaction(tracked, result) => f(result)(tracked)
    }
  }

  def >>=[B](f: A => Behavior[B]) = flatMap(f)

  def andThen[B](f: Behavior[B]) = this >>= (_ => f)

  def execute = apply(TrackedEventSources())

  def result = execute match {
    case Reaction(_, result) => result
  }
}

object Behavior {
  def apply[Result](callback: TrackedEventSources => Reaction[Result]) = new Behavior[Result] {
    def apply(tracked: TrackedEventSources) = callback(tracked)
  }
}
