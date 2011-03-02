package com.zilverline.es2
package transaction

case class EventSourceState[+A](id: Identifier, original: Revision, current: Revision, value: A, changes: IndexedSeq[DomainEvent]) {
  def modify[B, E <: DomainEvent](event: E)(callback: Uncommitted[E] => B): EventSourceState[B] = {
    val nextRevision = current + 1
    val updatedValue = callback(Uncommitted(id, nextRevision, event))
    copy(current = nextRevision, value = updatedValue, changes = changes :+ event)
  }
}

case class UnitOfWork(eventSources: Map[Identifier, EventSourceState[Any]] = Map.empty) {

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

case class TransactionState[+A](uow: UnitOfWork, result: A) {
  def changes(source: Identifier): Seq[DomainEvent] = uow.eventSources.get(source).map(_.changes).getOrElse(Seq.empty)
}

trait Transaction[+A] {
  def apply(uow: UnitOfWork): TransactionState[A]

  def map[B](f: A => B) = flatMap(a => pure(f(a)))

  def flatMap[B](f: A => Transaction[B]) = Transaction {uow =>
    this(uow) match {
      case TransactionState(uow, result) => f(result)(uow)
    }
  }

  def >>=[B](f: A => Transaction[B]) = flatMap(f)

  def andThen[B](f: Transaction[B]) = this >>= (_ => f)

  def trigger = apply(UnitOfWork())

  def result = trigger match {
    case TransactionState(_, result) => result
  }
}

object Transaction {
  def apply[Result](callback: UnitOfWork => TransactionState[Result]) = new Transaction[Result] {
    def apply(uow: UnitOfWork) = callback(uow)
  }
}
