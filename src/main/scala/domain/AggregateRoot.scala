package com.zilverline.es2
package domain

import behavior._

trait EventSourced {
  def applyEvent: PartialFunction[CommittedEvent, EventSourced]
}

trait AggregateRoot extends EventSourced {
  type Event <: DomainEvent

  protected def id: Identifier

  protected class EventHandler[A <: DomainEvent, +B](callback: Recorded[A] => B) {
    def apply(event: A) = record(id, event) flatMap (recorded => accept(callback(recorded)))

    def applyFromHistory(event: Committed[A]) = callback(event)
  }

  protected def recorded[A <: Event, B](callback: Recorded[A] => B) = new EventHandler(callback)

  protected def handler[A <: Event, B](callback: A => B) = recorded {
    recorded: Recorded[A] => callback(recorded.event)
  }

  implicit protected def handlerToPartialFunction[A <: DomainEvent, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[CommittedEvent, B] {
      def apply(committed: CommittedEvent) =
        if (isDefinedAt(committed)) handler.applyFromHistory(committed.asInstanceOf[Committed[A]])
        else error("unhandled event " + committed + " for " + this)

      def isDefinedAt(committed: CommittedEvent) = m.erasure.isInstance(committed.event)
    }
}

trait AggregateFactory[AR <: AggregateRoot] extends EventSourced {
  protected class EventHandler[A <: DomainEvent, +B](callback: Recorded[A] => B) {
    def apply(source: Identifier, event: A) = record(source, event) flatMap (recorded => accept(callback(recorded)))

    def applyFromHistory(event: Committed[A]) = callback(event)
  }

  protected def handler[A <: AR#Event, B](callback: Identifier => A => B) = new EventHandler[A, B]({
    recorded => callback(recorded.source)(recorded.event)
  })

  implicit protected def handlerToPartialFunction[A <: DomainEvent, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[CommittedEvent, B] {
      def apply(committed: CommittedEvent) =
        if (isDefinedAt(committed)) handler.applyFromHistory(committed.asInstanceOf[Committed[A]])
        else error("unhandled event " + committed + " for " + this)

      def isDefinedAt(committed: CommittedEvent) = m.erasure.isInstance(committed.event)
    }

  def loadFromHistory[T <: AR](history: Iterable[CommittedEvent]): T = {
    var aggregate = applyEvent(history.head)
    (aggregate /: history.tail)(_.applyEvent(_)).asInstanceOf[T]
  }
}
