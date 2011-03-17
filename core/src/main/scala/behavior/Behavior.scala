package com.zilverline.es2
package behavior

private[behavior] case class TrackedEventSource[+A](
  id: Identifier,
  originalRevision: Revision,
  currentValue: A,
  changes: IndexedSeq[DomainEvent] = IndexedSeq.empty
  ) {
  def currentRevision = originalRevision + changes.size

  def modify[E <: DomainEvent, B](event: E)(callback: Uncommitted[E] => B): TrackedEventSource[B] = {
    val updatedValue = callback(Uncommitted(id, currentRevision + 1, event))
    copy(currentValue = updatedValue, changes = changes :+ event)
  }
}

private[behavior] case class TrackedEventSources(eventSources: Map[Identifier, TrackedEventSource[Any]] = Map.empty) {
  def getEventSource(eventSourceId: Identifier): Option[Any] = eventSources.get(eventSourceId).map(_.currentValue)

  def trackEventSource(eventSourceId: Identifier, originalRevision: Revision, value: Any): Reaction[Unit] = {
    require(!eventSources.contains(eventSourceId), "already tracking " + eventSourceId)
    Reaction(
      copy(eventSources + (eventSourceId -> TrackedEventSource(eventSourceId, originalRevision, value))),
      ())
  }

  def modifyEventSource[A <: DomainEvent, B](eventSourceId: Identifier, event: A)(callback: Uncommitted[A] => B): Reaction[B] = {
    val originalState = eventSources.getOrElse(eventSourceId, TrackedEventSource(eventSourceId, InitialRevision, ()))
    val updatedState = originalState.modify(event)(callback)
    Reaction(copy(eventSources.updated(eventSourceId, updatedState)), updatedState.currentValue)
  }
}

case class Reaction[+A](tracked: TrackedEventSources, result: A)

trait Behavior[+A] {
  def apply(tracked: TrackedEventSources): Reaction[A]

  def map[B](f: A => B): Behavior[B] = flatMap(a => pure(f(a)))

  def flatMap[B](f: A => Behavior[B]): Behavior[B] = Behavior {tracked =>
    this(tracked) match {
      case Reaction(tracked, result) => f(result)(tracked)
    }
  }

  def >>=[B](f: A => Behavior[B]): Behavior[B] = flatMap(f)

  def andThen[B](f: Behavior[B]): Behavior[B] = this >>= (_ => f)

  def execute = apply(TrackedEventSources())
}

object Behavior {
  def apply[A](callback: TrackedEventSources => Reaction[A]) = new Behavior[A] {
    def apply(tracked: TrackedEventSources) = callback(tracked)
  }
}
