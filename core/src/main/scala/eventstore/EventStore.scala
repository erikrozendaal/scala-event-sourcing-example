package com.zilverline.es2
package eventstore

class OptimisticLockingException(message: String) extends RuntimeException(message)

case class Commit(source: Identifier, revision: Revision, events: Seq[DomainEvent])

trait EventStore {
  type EventStoreListener = CommittedEvent => Unit

  def commit(attempt: Commit)

  def load(source: Identifier): Seq[CommittedEvent]

  def addListener(callback: EventStoreListener) {
    synchronized {listeners = listeners :+ callback}
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

  @volatile
  private var listeners: IndexedSeq[EventStoreListener] = IndexedSeq.empty
}
