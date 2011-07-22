package com.zilverline.es2
package eventstore

import util.Logging

class OptimisticLockingException(message: String) extends RuntimeException(message)

case class Commit(source: Identifier, revision: Revision, events: Seq[DomainEvent])

trait EventStore {
  type EventStoreListener = CommittedEvent => Unit

  def commit(attempt: Commit)
  
  protected def listeners: Seq[EventStoreListener]

  def load(source: Identifier): Seq[CommittedEvent]

  def replayAllEvents() {
  }

  protected[this] def makeCommittedEvents(commit: Commit) = {
    commit.events.zipWithIndex map {
      case (event, index) => Committed(commit.source, commit.revision + 1 + index, event)
    }
  }

  protected[this] def dispatchEvents(events: Iterable[CommittedEvent]) {
    val listeners = this.listeners
    for (event <- events; listener <- listeners) listener(event)
  }
}

trait LoggingEventStore extends EventStore with Logging {

  abstract override def commit(attempt: Commit) {
    profile("commit " + attempt) {
      super.commit(attempt)
    }
  }

  abstract override def replayAllEvents() {
    profile("replayed all events") {
      super.replayAllEvents
    }
  }

}
