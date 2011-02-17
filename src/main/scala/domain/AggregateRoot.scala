package com.zilverline.es2
package domain

import behavior._

trait EventSourced {
  def applyEvent: PartialFunction[Recorded[DomainEvent], EventSourced]
}

trait AggregateRoot extends EventSourced {
  type Event <: DomainEvent

  protected def id: Identifier

  protected class EventHandler[A <: DomainEvent, +B](callback: Recorded[A] => B) {
    def apply(event: A) = record(id, event) flatMap (recorded => accept(callback(recorded)))

    def applyFromHistory(event: Recorded[A]) = callback(event)
  }

  protected def handler[A <: Event, B](callback: A => B) = new EventHandler({
    recorded: Recorded[A] => callback(recorded.event)
  })

  protected def unhandled = new EventHandler({event: DomainEvent =>
    error("unhandled event " + event + " for " + this)
  })

  implicit protected def handlerToPartialFunction[A <: DomainEvent, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[Recorded[DomainEvent], B] {
      def apply(recorded: Recorded[DomainEvent]) =
        if (isDefinedAt(recorded)) handler.applyFromHistory(recorded.asInstanceOf[Recorded[A]])
        else unhandled.applyFromHistory(recorded)

      def isDefinedAt(recorded: Recorded[DomainEvent]) = m.erasure.isInstance(recorded.event)
    }
}

trait AggregateFactory[AR <: AggregateRoot] extends EventSourced {
  protected class EventHandler[A <: DomainEvent, +B](callback: Recorded[A] => B) {
    def apply(source: Identifier, event: A) = record(source, event) flatMap (recorded => accept(callback(recorded)))

    def applyFromHistory(event: Recorded[A]) = callback(event)
  }

  protected def handler[A <: AR#Event, B](callback: (Identifier, A) => B) = new EventHandler({
    recorded: Recorded[A] => callback(recorded.source, recorded.event)
  })

  protected def unhandled = new EventHandler[DomainEvent, Nothing]({
    event =>
      error("unhandled event " + event + " for " + this)
  })

  implicit protected def handlerToPartialFunction[A <: DomainEvent, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[Recorded[DomainEvent], B] {
      def apply(event: Recorded[DomainEvent]) =
        if (isDefinedAt(event)) handler.applyFromHistory(event.asInstanceOf[Recorded[A]])
        else unhandled.applyFromHistory(event)

      def isDefinedAt(event: Recorded[DomainEvent]) = m.erasure.isInstance(event.event)
    }

  def loadFromHistory[T <: AR](history: Iterable[Committed[DomainEvent]]): T = {
    val aggregate = applyEvent(history.head)
    (aggregate /: history.tail)(_.applyEvent(_)).asInstanceOf[T]
  }
}
