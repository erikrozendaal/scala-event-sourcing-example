package com.zilverline.es2
package eventstore

import scala.collection.mutable.{Map => MMap}

class MemoryEventStore(dispatcher: Seq[CommittedEvent] => Unit) extends EventStore {

  def commit(attempt: Commit) {
    if (attempt.events.isEmpty) return

    synchronized {
      val stored = storedEvents.getOrElse(attempt.source, Vector.empty)
      val actual = stored.size
      if (attempt.revision != actual)
        throw new OptimisticLockingException("sequence number does not match. Required <" + actual + "> was <" + attempt.revision  + ">")

      val committed = makeCommittedEvents(attempt)
      storedEvents.put(attempt.source, stored ++ committed)
      dispatcher(committed)
    }
  }

  def load(source: Identifier): Seq[CommittedEvent] = synchronized {
    storedEvents.getOrElse(source, Seq.empty)
  }

  private val storedEvents: MMap[Identifier, Vector[CommittedEvent]] = MMap.empty
}
