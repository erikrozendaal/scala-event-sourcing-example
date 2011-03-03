package com.zilverline.es2
package domain

import transaction._

class AggregateEventHandler[-A <: DomainEvent, +B](source: Identifier, callback: Recorded[A] => B) {
  def apply(event: A) = modifyEventSource(source, event)(callback)

  private[domain] def applyFromHistory(event: Recorded[A]): B = callback(event)
}

object AggregateEventHandler {
  implicit def asPartialFunction[A <: DomainEvent, B](handler: AggregateEventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[RecordedEvent, B] {
      def apply(recorded: RecordedEvent) =
        if (isDefinedAt(recorded)) handler.applyFromHistory(recorded.asInstanceOf[Committed[A]])
        else error("unhandled event " + recorded + " for " + this)

      def isDefinedAt(recorded: RecordedEvent) = m.erasure.isInstance(recorded.payload)
    }
}

trait AggregateRoot {
  protected[this] type Event <: DomainEvent

  protected[this] def id: Identifier

  protected[this] def applyEvent: RecordedEvent PartialFunction AggregateRoot

  protected[this] def when[A <: Event] = new When[A]

  implicit protected[this] def recordedToPayload[A <: Event](recorded: Recorded[A]): A = recorded.payload

  protected[this] class When[A <: DomainEvent] {
    def apply[B](callback: Recorded[A] => B) = new AggregateEventHandler(id, callback)
  }

  /* APIs for internal use */
  private[domain] type InternalEvent = Event

  private[domain] def internalId = id

  private[domain] def internalApplyEvent = applyEvent
}

class AggregateRepository[-AR <: AggregateRoot](aggregates: Aggregates) {
  def get[T <: AR](id: Identifier): Transaction[T] = Transaction {
    uow =>
      uow.getEventSource(id) map {
        result => TransactionState(uow, result.asInstanceOf[T])
      } getOrElse {
        aggregates.get(id) map {
          result =>
            TransactionState(uow.trackEventSource(id, result._1, result._2), result._2.asInstanceOf[T])
        } getOrElse {
          throw new IllegalArgumentException("event source <" + id + "> not found")
        }
      }
  }

  def update[A <: AR, B <: AR](id: Identifier)(f: A => Transaction[B]): Transaction[B] = get(id) >>= f
}
