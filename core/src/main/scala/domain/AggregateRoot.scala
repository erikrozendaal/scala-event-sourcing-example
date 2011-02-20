package com.zilverline.es2
package domain

import behavior._

class RecordedEventHandler[-A <: DomainEvent, +B](callback: Recorded[A] => B) {
  def applyFromHistory(event: Recorded[A]): B = callback(event)
}

object RecordedEventHandler {
  implicit def asPartialFunction[A <: DomainEvent, B](handler: RecordedEventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[RecordedEvent, B] {
      def apply(recorded: RecordedEvent) =
        if (isDefinedAt(recorded)) handler.applyFromHistory(recorded.asInstanceOf[Committed[A]])
        else error("unhandled event " + recorded + " for " + this)

      def isDefinedAt(recorded: RecordedEvent) = m.erasure.isInstance(recorded.payload)
    }
}

class CreateHandler[-A <: DomainEvent, +B](callback: Recorded[A] => B) extends RecordedEventHandler(callback) {
  def apply(source: Identifier, event: A) = modifyEventSource(source, event)(callback)
}

class UpdateHandler[-A <: DomainEvent, +B](source: Identifier, callback: Recorded[A] => B) extends RecordedEventHandler(callback) {
  def apply(event: A) = modifyEventSource(source, event)(callback)
}

trait AggregateRoot {
  protected[this] type Event <: DomainEvent

  protected[this] def id: Identifier

  protected[this] def applyEvent: RecordedEvent PartialFunction AggregateRoot

  protected[this] def when[A <: Event] = new When[A]

  implicit protected[this] def recordedToPayload[A <: Event](recorded: Recorded[A]): A = recorded.payload

  protected[this] class When[A <: DomainEvent] {
    def apply[B](callback: Recorded[A] => B) = new UpdateHandler(id, callback)
  }

  private[domain] def internalId = id
  private[domain] type InternalEvent = Event
  private[domain] def internalApplyEvent = applyEvent
}

trait AggregateFactory[-AR <: AggregateRoot] {
  def loadFromHistory[T <: AR : NotNothing](history: Iterable[CommittedEvent]): T = {
    val aggregate = applyEvent(history.head)
    (aggregate /: history.tail)(_.internalApplyEvent(_)).asInstanceOf[T]
  }

  protected[this] def applyEvent: RecordedEvent PartialFunction AggregateRoot

  protected[this] def when[A <: AR#InternalEvent] = new When[A]

  implicit protected[this] def recordedToPayload[A <: AR#InternalEvent](recorded: Recorded[A]): A = recorded.payload

  protected[this] class When[A <: DomainEvent] {
    def apply[B](callback: Recorded[A] => B) = new CreateHandler(callback)
  }

  private[domain] def internalApplyEvent = applyEvent
}
