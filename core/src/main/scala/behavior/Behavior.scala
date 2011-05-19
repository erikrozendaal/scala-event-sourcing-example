package com.zilverline.es2
package behavior

import util.NotNothing

private[behavior] case class TrackedEventSource[+T](
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

private[behavior] case class Session(tracked: Map[Identifier, TrackedEventSource[Any]] = Map.empty) {
  def value[A: NotNothing](eventSourceId: Identifier): Option[A] = tracked.get(eventSourceId).map(_.value.asInstanceOf[A])

  def track(eventSourceId: Identifier, revision: Revision, value: Any): Session = {
    require(!tracked.contains(eventSourceId), "already tracking " + eventSourceId)
    copy(tracked.updated(eventSourceId, TrackedEventSource(eventSourceId, revision, value)))
  }

  def record[A <: DomainEvent, B](eventSourceId: Identifier, event: A)(handler: Uncommitted[A] => B): Reaction[B] = {
    val originalState = tracked.getOrElse(eventSourceId, TrackedEventSource(eventSourceId, InitialRevision, ()))
    val updatedState = originalState.record(event)(handler)
    Reaction(copy(tracked.updated(eventSourceId, updatedState)), updatedState.value)
  }
}

case class Reaction[+A](session: Session, result: A)

trait Behavior[+A] {
  def apply(session: Session): Reaction[A]

  def map[B](f: A => B): Behavior[B] = Behavior {
    session =>
      val reaction = this(session)
      Reaction(reaction.session, f(reaction.result))
  }

  def flatMap[B](f: A => Behavior[B]): Behavior[B] = Behavior {
    session =>
      val reaction = this(session)
      f(reaction.result)(reaction.session)
  }

  def then[B](next: Behavior[B]): Behavior[B] = flatMap(_ => next)
}

object Behavior {
  def run[A](behavior: Behavior[A]): Reaction[A] = behavior(Session())

  def pure[A](a: A): Behavior[A] = Behavior {Reaction(_, a)}

  def getTrackedEventSource[A](eventSourceId: Identifier): Behavior[Option[A]] = Behavior {
    session => Reaction(session, session.value[A](eventSourceId))
  }

  def trackEventSource[A](eventSourceId: Identifier, revision: Revision, value: A): Behavior[A] = Behavior {
    session => Reaction(session.track(eventSourceId, revision, value), value)
  }

  def record[A <: DomainEvent, B](source: Identifier, event: A)(handler: Uncommitted[A] => B): Behavior[B] = Behavior {
    session => session.record(source, event)(handler)
  }

  private def apply[A](f: Session => Reaction[A]): Behavior[A] = new Behavior[A] {
    def apply(tracked: Session) = f(tracked)
  }
}
