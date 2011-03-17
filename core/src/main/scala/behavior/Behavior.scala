package com.zilverline.es2
package behavior

import scala.util.DynamicVariable

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
  def getValue(eventSourceId: Identifier): Option[Any] = eventSources.get(eventSourceId).map(_.currentValue)

  def track(eventSourceId: Identifier, originalRevision: Revision, value: Any): TrackedEventSources = {
    require(!eventSources.contains(eventSourceId), "already tracking " + eventSourceId)
    copy(eventSources + (eventSourceId -> TrackedEventSource(eventSourceId, originalRevision, value)))
  }

  def modify[A <: DomainEvent, B](eventSourceId: Identifier, event: A)(callback: Uncommitted[A] => B): Reaction[B] = {
    val originalState = eventSources.getOrElse(eventSourceId, TrackedEventSource(eventSourceId, InitialRevision, ()))
    val updatedState = originalState.modify(event)(callback)
    Reaction(copy(eventSources.updated(eventSourceId, updatedState)), updatedState.currentValue)
  }
}

case class Reaction[+A](tracked: TrackedEventSources, result: A)

object Behavior {
  def run[A](behavior: => A): Reaction[A] = current.withValue(Some(TrackedEventSources())) {
    val value = behavior
    Reaction(current.value.get, value)
  }

  def getEventSource(eventSourceId: Identifier): Option[Any] = Behavior {tracked =>
    Reaction(tracked, tracked.getValue(eventSourceId))
  }

  def trackEventSource(eventSourceId: Identifier, revision: Revision, value: Any) {
    Behavior(tracked => Reaction(tracked.track(eventSourceId, revision, value), ()))
  }

  def modifyEventSource[A <: DomainEvent, B](source: Identifier, event: A)(f: Uncommitted[A] => B): B = Behavior {
    uow => uow.modify(source, event)(f)
  }

  private def apply[A](callback: TrackedEventSources => Reaction[A]): A = {
    val reaction = callback(current.value.getOrElse(error("no behavior active on current thread")))
    current.value = Some(reaction.tracked)
    reaction.result
  }

  private[behavior] object current extends DynamicVariable[Option[TrackedEventSources]](None)
}
