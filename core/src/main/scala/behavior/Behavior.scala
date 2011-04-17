package com.zilverline.es2
package behavior

import Behavior._

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
  def getCurrentValue(eventSourceId: Identifier): Option[Any] = eventSources.get(eventSourceId).map(_.currentValue)

  def track(eventSourceId: Identifier, revision: Revision, value: Any): TrackedEventSources = {
    require(!eventSources.contains(eventSourceId), "already tracking " + eventSourceId)
    copy(eventSources + (eventSourceId -> TrackedEventSource(eventSourceId, revision, value)))
  }

  def modify[A <: DomainEvent, B](eventSourceId: Identifier, event: A)(callback: Uncommitted[A] => B): Reaction[B] = {
    val originalState = eventSources.getOrElse(eventSourceId, TrackedEventSource(eventSourceId, InitialRevision, ()))
    val updatedState = originalState.modify(event)(callback)
    Reaction(copy(eventSources.updated(eventSourceId, updatedState)), updatedState.currentValue)
  }
}

case class Reaction[+A](tracked: TrackedEventSources, result: A)

trait Behavior[+A] {
  def apply(tracked: TrackedEventSources): Reaction[A]

  def map[B](f: A => B): Behavior[B] = flatMap {x => pure(f(x))}

  def flatMap[B](f: A => Behavior[B]): Behavior[B] = Behavior {tracked =>
    val reaction = apply(tracked)
    f(reaction.result)(reaction.tracked)
  }

  def then[B](next: Behavior[B]): Behavior[B] = flatMap(_ => next)
}

object Behavior {
  private def apply[A](f: TrackedEventSources => Reaction[A]): Behavior[A] = new Behavior[A] {
    def apply(tracked: TrackedEventSources) = f(tracked)
  }

  def run[A](behavior: Behavior[A]): Reaction[A] = behavior(TrackedEventSources())

  def pure[A](a: A): Behavior[A] = Behavior(Reaction(_, a))

  def getTrackedEventSource(eventSourceId: Identifier): Behavior[Option[Any]] = Behavior {tracked =>
    Reaction(tracked, tracked.getCurrentValue(eventSourceId))
  }

  def trackEventSource[A](eventSourceId: Identifier, revision: Revision, value: A): Behavior[A] = Behavior {tracked =>
    Reaction(tracked.track(eventSourceId, revision, value), value)
  }

  def modifyEventSource[A <: DomainEvent, B](source: Identifier, event: A)(f: Uncommitted[A] => B): Behavior[B] = Behavior {
    uow => uow.modify(source, event)(f)
  }
}
