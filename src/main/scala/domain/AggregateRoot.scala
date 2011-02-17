package com.zilverline.es2
package domain

import events._

case class UnitOfWork(events: List[UncommittedEvent[DomainEvent]], eventStore: storage.EventStore) {
  def record(aggregate: Identifier, event: DomainEvent) = copy(UncommittedEvent(aggregate, event) :: events)
}

trait Reaction[+T]
case class Accepted[+T](uow: UnitOfWork, result: T) extends Reaction[T]
case class Rejected(message: String) extends Reaction[Nothing]

object Behaviors {
  def behavior[T](callback: UnitOfWork => Reaction[T]) = new Behavior[T] {
    def apply(uow: UnitOfWork) = callback(uow)
  }

  def accept[T](result: T) = behavior(uow => Accepted(uow, result))

  def reject(message: String) = behavior(_ => Rejected(message))

  def record[A <: DomainEvent](source: Identifier, event: A) = behavior {
    uow =>
      val uncommitted = UncommittedEvent(source, event)
      Accepted(uow.copy(uncommitted :: uow.events), uncommitted)
  }

  def load[T <: AggregateRoot](factory: AggregateFactory[_], source: Identifier): Behavior[T] = behavior {
    uow =>
      val events = uow.eventStore.load(source)
      Accepted(uow, factory.loadFromHistory(events))
  }

  def guard(condition: Boolean, message: => String) =
    if (condition) accept() else reject(message)
}

import Behaviors._

trait Behavior[+A] {
  def apply(uow: UnitOfWork): Reaction[A]

  def map[B](f: A => B) = flatMap(a => accept(f(a)))

  def flatMap[B](next: A => Behavior[B]) = behavior {uow =>
    this(uow) match {
      case Accepted(uow, result) => next(result)(uow)
      case Rejected(message) => Rejected(message)
    }
  }

  def andThen[B](next: Behavior[B]) = this flatMap (_ => next)
}

trait EventSourced {
  def applyEvent: PartialFunction[RecordedEvent[DomainEvent], EventSourced]
}

trait AggregateRoot extends EventSourced {
  protected def id: Identifier

  protected class EventHandler[Event <: DomainEvent, +Result](callback: RecordedEvent[Event] => Result) {
    def apply(event: Event) = record(id, event) flatMap (recorded => accept(callback(recorded)))

    def applyFromHistory(event: RecordedEvent[Event]) = callback(event)
  }

  protected def handler[A <: DomainEvent, B](callback: A => B) = new EventHandler((recorded: RecordedEvent[A]) => callback(recorded.payload))

  protected def unhandled = handler {event: AnyRef =>
    error("unhandled event " + event + " for " + this)
  }

  implicit protected def handlerToPartialFunction[A <: DomainEvent, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[RecordedEvent[DomainEvent], B] {
      def apply(event: RecordedEvent[DomainEvent]) =
        if (isDefinedAt(event)) handler.applyFromHistory(event.asInstanceOf[RecordedEvent[A]])
        else unhandled.applyFromHistory(event)

      def isDefinedAt(event: RecordedEvent[DomainEvent]) = m.erasure.isInstance(event.payload)
    }
}

trait AggregateFactory[AR <: AggregateRoot] extends EventSourced {
  protected class EventHandler[Event <: DomainEvent, +Result](callback: RecordedEvent[Event] => Result) {
    def apply(source: Identifier, event: Event) = record(source, event) flatMap (recorded => accept(callback(recorded)))

    def applyFromHistory(event: RecordedEvent[Event]) = callback(event)
  }

  protected def handler[A <: DomainEvent, B](callback: (Identifier, A) => B) = new EventHandler({
    recorded: RecordedEvent[A] => callback(recorded.source, recorded.payload)
  })

  protected def unhandled = handler {(source: Identifier, event: DomainEvent) =>
    error("unhandled event " + event + " for " + source + " using factory " + this)
  }

  implicit protected def handlerToPartialFunction[A <: DomainEvent, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) =
    new PartialFunction[RecordedEvent[DomainEvent], B] {
      def apply(event: RecordedEvent[DomainEvent]) =
        if (isDefinedAt(event)) handler.applyFromHistory(event.asInstanceOf[RecordedEvent[A]])
        else unhandled.applyFromHistory(event)

      def isDefinedAt(event: RecordedEvent[DomainEvent]) = m.erasure.isInstance(event.payload)
    }

  def loadFromHistory[T <: AR](history: Iterable[CommittedEvent[DomainEvent]]): T = {
    val aggregate = applyEvent(history.head)
    (aggregate /: history.tail)(_.applyEvent(_)).asInstanceOf[T]
  }
}
