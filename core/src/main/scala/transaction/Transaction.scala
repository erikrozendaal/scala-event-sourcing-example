package com.zilverline.es2
package transaction

private[transaction] case class TrackedEventSource[+A](
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

private[transaction] case class TrackedEventSources(eventSources: Map[Identifier, TrackedEventSource[Any]] = Map.empty) {
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

case class TransactionState[+A](tracked: TrackedEventSources, result: A)

trait Transaction[+A] {
  def apply(tracked: TrackedEventSources): TransactionState[A]

  def map[B](f: A => B) = flatMap(a => pure(f(a)))

  def flatMap[B](f: A => Transaction[B]) = Transaction {tracked =>
    this(tracked) match {
      case TransactionState(tracked, result) => f(result)(tracked)
    }
  }

  def >>=[B](f: A => Transaction[B]) = flatMap(f)

  def andThen[B](f: Transaction[B]) = this >>= (_ => f)

  def execute = apply(TrackedEventSources())

  def result = execute match {
    case TransactionState(_, result) => result
  }
}

object Transaction {
  def apply[Result](callback: TrackedEventSources => TransactionState[Result]) = new Transaction[Result] {
    def apply(tracked: TrackedEventSources) = callback(tracked)
  }
}
