package com.zilverline.es2
package eventstore

import scala.collection.mutable.{Map => MMap}
import scala.collection.immutable.Queue

class MemoryEventStore extends EventStore {

  def commit(events: Iterable[UncommittedEvent]) {
    if (events.isEmpty) return
    verifyEventsBeforeCommit(events)

    synchronized {
      val first = events.head
      val stored = storedEvents.getOrElse(first.source, Queue.empty)
      if (stored.size + 1 != first.sequence)
        throw new OptimisticLockingException("sequence number does not match expected <" + stored.size + "> was <" + first.sequence + ">")

      val committed = events.map(event => Committed(event.source, event.sequence, event.payload))
      storedEvents.put(first.source, stored ++ committed)
      dispatchEvents(committed)
    }
  }

  def load(source: Identifier): Iterable[CommittedEvent] = synchronized {
    storedEvents.getOrElse(source, Iterable.empty)
  }

  private val storedEvents: MMap[Identifier, Queue[CommittedEvent]] = MMap.empty
}
