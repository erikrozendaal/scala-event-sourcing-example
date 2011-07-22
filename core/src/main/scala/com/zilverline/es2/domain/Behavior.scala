package com.zilverline.es2
package domain

import util.NotNothing

case class Reaction[+A](session: Session, result: A)

trait Behavior[+A] {
  def apply(session: Session): Reaction[A]

  def map[B](f: A => B): Behavior[B] = Behavior {
    session =>
      val reaction = apply(session)
      reaction.copy(result = f(reaction.result))
  }

  def flatMap[B](f: A => Behavior[B]): Behavior[B] = Behavior {
    session =>
      val reaction = apply(session)
      f(reaction.result)(reaction.session)
  }

  def then[B](next: => Behavior[B]): Behavior[B] = flatMap(_ => next)
}

object Behavior {
  def pure[A](a: A): Behavior[A] = Behavior {Reaction(_, a)}

  def trackEventSource[A](eventSourceId: Identifier, revision: Revision, value: A): Behavior[A] = Behavior {
    session => Reaction(session.track(eventSourceId, revision, value), value)
  }

  def record[A <: DomainEvent, B](event: A)(handler: Uncommitted[A] => B): Behavior[B] = Behavior {
    session => session.record(event)(handler)
  }

  def currentAggregateId: Behavior[Identifier] = Behavior {
    session => Reaction(session, session.currentAggregateId)
  }

  private[domain] def apply[A](f: Session => Reaction[A]): Behavior[A] = new Behavior[A] {
    def apply(session: Session) = f(session)
  }

  private[domain] def getAggregate(id: Identifier): Behavior[Option[Aggregate]] = Behavior {
    session => Reaction(session, session.aggregates.get(id))
  }

  private[domain] def getTrackedEventSource[A : NotNothing](eventSourceId: Identifier): Behavior[Option[A]] = Behavior {
    session => Reaction(session, session.value(eventSourceId))
  }
}

case class Reference[+A](aggregateId: Identifier) {
  def modify[B](f: A => Behavior[B]): Behavior[B] = get flatMap (aggregate => Behavior {
    session =>
      val reaction = f(aggregate)(session.copy(currentAggregateId = aggregateId))
      reaction.copy(session = reaction.session.copy(currentAggregateId = session.currentAggregateId))
  })

  def get: Behavior[A] = {
    import Behavior._
    getTrackedEventSource[A](aggregateId).flatMap {
      case Some(aggregate) =>
        pure(aggregate)
      case None =>
        getAggregate(aggregateId).flatMap {
          case Some(aggregate) =>
            trackEventSource(aggregate.id, aggregate.revision, aggregate.root.asInstanceOf[A])
          case None =>
            sys.error("unknown aggregate <" + aggregateId + ">")
        }
    }
  }
}

case class TrackedEventSource[+T](
  id: Identifier,
  revision: Revision,
  value: T,
  changes: IndexedSeq[DomainEvent] = IndexedSeq.empty
  ) {
  def record[A <: DomainEvent, B](event: A)(handler: Uncommitted[A] => B): TrackedEventSource[B] = {
    val updatedValue = handler(Uncommitted(id, event))
    copy(value = updatedValue, changes = changes :+ event)
  }
}

case class Session(currentAggregateId: Identifier, aggregates: StoresAggregates, tracked: Map[Identifier, TrackedEventSource[Any]] = Map.empty) {
  def value[A: NotNothing](eventSourceId: Identifier): Option[A] = tracked.get(eventSourceId).map(_.value.asInstanceOf[A])

  def track(eventSourceId: Identifier, revision: Revision, value: Any): Session = {
    require(!tracked.contains(eventSourceId), "already tracking " + eventSourceId)
    copy(tracked = tracked.updated(eventSourceId, TrackedEventSource(eventSourceId, revision, value)))
  }

  def record[A <: DomainEvent, B](event: A)(handler: Uncommitted[A] => B): Reaction[B] = {
    val originalState = tracked.getOrElse(currentAggregateId, TrackedEventSource(currentAggregateId, InitialRevision, ()))
    val updatedState = originalState.record(event)(handler)
    Reaction(copy(tracked = tracked.updated(currentAggregateId, updatedState)), updatedState.value)
  }
}
