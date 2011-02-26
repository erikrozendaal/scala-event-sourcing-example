package com.zilverline.es2
package eventstore

import scala.collection.mutable.{Map => MMap}

class MemoryEventStore extends EventStore {

  def commit(attempt: Commit) {
    if (attempt.events.isEmpty) return

    synchronized {
      val stored = storedEvents.getOrElse(attempt.source, IndexedSeq.empty)
      val actual = stored.size
      if (attempt.revision != actual)
        throw new OptimisticLockingException("sequence number does not match expected <" + attempt.revision + "> was <" + actual + ">")

      val committed = makeCommittedEvents(attempt)
      storedEvents.put(attempt.source, stored ++ committed)
      dispatchEvents(committed)
    }
  }

  def load(source: Identifier): Seq[CommittedEvent] = synchronized {
    storedEvents.getOrElse(source, Seq.empty)
  }

  private val storedEvents: MMap[Identifier, IndexedSeq[CommittedEvent]] = MMap.empty
}
