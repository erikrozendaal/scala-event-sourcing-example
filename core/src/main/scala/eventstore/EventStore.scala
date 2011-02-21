package com.zilverline.es2
package eventstore

import collection.immutable.Queue

class OptimisticLockingException(message: String) extends RuntimeException(message)

trait EventStore {
  type EventStoreListener = CommittedEvent => Unit
  def commit(events: Iterable[UncommittedEvent])
  def load(source: Identifier): Iterable[CommittedEvent]

  def addListener(callback: EventStoreListener) {
    synchronized {listeners = listeners :+ callback}
  }

  protected[this] def dispatchEvents(events: Iterable[CommittedEvent]) {
    val listeners = this.listeners
    for (event <- events; listener <- listeners) listener(event)
  }

  protected[this] def verifyEventsBeforeCommit(events: Iterable[UncommittedEvent]): Unit = {
    require(events.forall(_.source == events.head.source), "only a single source can be updated in a single commit")
    require(events.zip(events.tail).forall(pair => pair._1.sequence + 1 == pair._2.sequence), "events must be in proper order")
  }

  @volatile
  private var listeners: Queue[EventStoreListener] = Queue.empty
}
