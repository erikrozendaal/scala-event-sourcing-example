package com.zilverline.es2
package behavior

case class UnitOfWork(events: List[UncommittedEvent], eventStore: eventstore.EventStore) {
  def record(aggregate: Identifier, event: DomainEvent) = copy(Uncommitted(aggregate, event) :: events)
}

trait Reaction[+T]
case class Accepted[+T](uow: UnitOfWork, result: T) extends Reaction[T]
case class Rejected(message: String) extends Reaction[Nothing]

trait Behavior[+A] {
  import Behavior._

  def apply(uow: UnitOfWork): Reaction[A]

  def map[B](f: A => B) = flatMap(a => accept(f(a)))

  def flatMap[B](next: A => Behavior[B]) = Behavior {uow =>
    this(uow) match {
      case Accepted(uow, result) => next(result)(uow)
      case Rejected(message) => Rejected(message)
    }
  }

  def andThen[B](next: Behavior[B]) = this flatMap (_ => next)
}

object Behavior {
  def apply[T](callback: UnitOfWork => Reaction[T]) = new Behavior[T] {
    def apply(uow: UnitOfWork) = callback(uow)
  }
}
