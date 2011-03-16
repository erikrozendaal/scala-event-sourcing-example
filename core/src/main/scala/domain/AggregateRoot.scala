package com.zilverline.es2
package domain

import transaction._, util._

trait AggregateEventHandler[-A <: DomainEvent, +B] {
  private[domain] def applyFromHistory(event: Recorded[A]): B
}

class AggregateRootEventHandler[-A <: DomainEvent, +B](aggregateId: Identifier, callback: Recorded[A] => B) extends AggregateEventHandler[A, B] {
  def apply(event: A) = modifyEventSource(aggregateId, event)(callback)

  private[domain] def applyFromHistory(event: Recorded[A]): B = callback(event)
}

class AggregateFactoryEventHandler[-A <: DomainEvent, +B](callback: Recorded[A] => B) extends AggregateEventHandler[A, B] {
  def apply(aggregateId: Identifier, event: A) = modifyEventSource(aggregateId, event)(callback)

  private[domain] def applyFromHistory(event: Recorded[A]): B = callback(event)
}

object AggregateEventHandler {
  implicit def partialFunctionFromAggregateEventHandler[A <: DomainEvent, B](handler: AggregateEventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[RecordedEvent, B] {
      def apply(recorded: RecordedEvent) =
        if (isDefinedAt(recorded)) handler.applyFromHistory(recorded.asInstanceOf[Committed[A]])
        else error("unhandled event " + recorded + " for " + this)

      def isDefinedAt(recorded: RecordedEvent) = m.erasure.isInstance(recorded.event)
    }
}

trait EventSource[ES <: EventSource[ES]] {
  protected[this] def applyEvent: PartialFunction[RecordedEvent, ES]

  private[domain] def internalApplyEvent = applyEvent
}

trait AggregateRoot extends EventSource[AggregateRoot] {
  protected[this] type Event <: DomainEvent

  protected[this] def id: Identifier

  protected[this] def when[A <: Event] = new When[A]

  implicit protected[this] def payloadOfRecordedEvent[A <: Event](recorded: Recorded[A]): A = recorded.event

  protected[this] class When[A <: DomainEvent] {
    def apply[B](callback: Recorded[A] => B) = new AggregateRootEventHandler(id, callback)
  }

  /* APIs for internal use */
  private[domain] type InternalEvent = Event
}

trait AggregateFactory[AR <: AggregateRoot] extends EventSource[AggregateRoot] {
  def loadFromHistory(history: Iterable[RecordedEvent]): AR = {
    val aggregate = applyEvent(history.head)
    (aggregate /: history.tail)(_.internalApplyEvent(_)).asInstanceOf[AR]
  }

  protected[this] type Event = AR#InternalEvent

  protected[this] def when[A <: Event] = new When[A]

  implicit protected[this] def payloadOfRecordedEvent[A <: Event](recorded: Recorded[A]): A = recorded.event

  protected[this] class When[A <: DomainEvent] {
    def apply[B](callback: Recorded[A] => B) = new AggregateFactoryEventHandler(callback)
  }
}

class AggregateRepository[-AR <: AggregateRoot : NotNothing](aggregates: Aggregates) {
  def get[T <: AR](id: Identifier): Transaction[T] = Transaction {
    uow =>
      uow.getEventSource(id) map {
        result => TransactionState(uow, result.asInstanceOf[T])
      } getOrElse {
        aggregates.get(id) map {
          aggregate =>
            TransactionState(uow.trackEventSource(id, aggregate.revision, aggregate.root), aggregate.root.asInstanceOf[T])
        } getOrElse {
          throw new IllegalArgumentException("aggregate <" + id + "> not found")
        }
      }
  }

  def update[A <: AR, B <: AR](id: Identifier)(f: A => Transaction[B]): Transaction[B] = get(id) >>= f
}
