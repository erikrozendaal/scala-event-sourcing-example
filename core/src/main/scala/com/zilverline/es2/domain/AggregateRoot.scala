package com.zilverline.es2
package domain

class AggregateEventHandler[-A <: DomainEvent, +B](callback: Recorded[A] => B) {
  def apply(event: A): Behavior[B] = Behavior.record(event)(callback)

  private[domain] def applyFromHistory(event: Recorded[A]): B = callback(event)
}

object AggregateEventHandler {
  implicit def partialFunctionFromAggregateEventHandler[A <: DomainEvent, B](handler: AggregateEventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[RecordedEvent, B] {
      def apply(recorded: RecordedEvent) =
        if (isDefinedAt(recorded)) handler.applyFromHistory(recorded.asInstanceOf[Committed[A]])
        else sys.error("unhandled event " + recorded + " for " + this)

      def isDefinedAt(recorded: RecordedEvent) = m.erasure.isInstance(recorded.payload)
    }
}

trait EventSource[ES <: EventSource[ES]] {
  protected[this] type Event <: DomainEvent

  protected[this] def applyEvent: PartialFunction[RecordedEvent, ES]

  protected[this] def when[A <: Event] = new When[A]

  protected[this] class When[A <: DomainEvent] {
    def apply[B](callback: Recorded[A] => B) = new AggregateEventHandler(callback)
  }

  implicit protected[this] def payloadOfRecordedEvent[A <: Event](recorded: Recorded[A]): A = recorded.payload

  /* APIs for internal use */
  private[domain] type InternalEvent = Event
  private[domain] def internalApplyEvent = applyEvent
}

trait AggregateRoot extends EventSource[AggregateRoot]

trait AggregateFactory[AR <: AggregateRoot] extends EventSource[AggregateRoot] {
  protected[this] type Event = AR#InternalEvent
}
