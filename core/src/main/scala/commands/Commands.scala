package com.zilverline.es2
package commands

import domain._
import eventstore.{Commit, EventStore}

trait Modify[A] {
  def execute[B](f: A => Behavior[B]): B
}

class Commands(eventStore: EventStore, aggregates: Aggregates) {
  def on[A](aggregateId: Identifier): Modify[A] = new Modify[A] {
    def execute[B](f: A => Behavior[B]): B = {
      val Reaction(session, result) = (Reference[A](aggregateId).modify(f))(Session(aggregateId, aggregates))
      commitChanges(session)
      result
    }
  }

  def create[A](aggregateId: Identifier)(f: => Behavior[A]): A = {
    val Reaction(session, result) = f(Session(aggregateId, aggregates))
    commitChanges(session)
    result
  }

  private def commitChanges(session: Session) {
    for (source <- session.tracked.values) {
      eventStore.commit(Commit(source.id, source.revision, source.changes))
    }
  }
}
