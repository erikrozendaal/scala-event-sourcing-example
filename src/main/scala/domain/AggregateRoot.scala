package com.zilverline.es2
package domain

import behavior._

trait AggregateRoot {
  protected[this] type Event <: DomainEvent

  protected[this] def id: Identifier

  protected[this] def applyEvent: CommittedEvent => AggregateRoot

  protected[this] def handler[A <: Event, B](callback: A => B) = new EventHandler(id, {
    recorded: Recorded[A] => callback(recorded.event)
  })

  protected class EventHandler[-A <: DomainEvent, +B](source: Identifier, callback: Recorded[A] => B) {
    def apply(event: A) = record(source, event) flatMap (recorded => accept(callback(recorded)))

    def applyFromHistory(event: Committed[A]) = callback(event)
  }

  implicit protected[this] def handlerToPartialFunction[A <: DomainEvent, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[CommittedEvent, B] {
      def apply(committed: CommittedEvent) =
        if (isDefinedAt(committed)) handler.applyFromHistory(committed.asInstanceOf[Committed[A]])
        else error("unhandled event " + committed + " for " + this)

      def isDefinedAt(committed: CommittedEvent) = m.erasure.isInstance(committed.event)
    }

  private[domain] type EventType = Event
  private[domain] def applyEventFromHistory = applyEvent
}

trait AggregateFactory[-AR <: AggregateRoot] {
  protected[this] def applyEvent: CommittedEvent => AggregateRoot

  protected[this] class EventHandler[A <: DomainEvent, +B](callback: Recorded[A] => B) {
    def apply(source: Identifier, event: A) = record(source, event) flatMap (recorded => accept(callback(recorded)))

    def applyFromHistory(event: Committed[A]) = callback(event)
  }

  protected[this] def create[A <: AR#EventType, B](callback: Identifier => A => B) = new EventHandler[A, B]({
    recorded => callback(recorded.source)(recorded.event)
  })

  implicit protected[this] def handlerToPartialFunction[A <: DomainEvent, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[CommittedEvent, B] {
      def apply(committed: CommittedEvent) =
        if (isDefinedAt(committed)) handler.applyFromHistory(committed.asInstanceOf[Committed[A]])
        else error("unhandled event " + committed + " for " + this)

      def isDefinedAt(committed: CommittedEvent) = m.erasure.isInstance(committed.event)
    }

  def loadFromHistory[T <: AR : NotNothing](history: Iterable[CommittedEvent]): T = {
    val aggregate = applyEvent(history.head)
    (aggregate /: history.tail)(_.applyEventFromHistory(_)).asInstanceOf[T]
  }
}
