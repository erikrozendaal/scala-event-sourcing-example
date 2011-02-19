package com.zilverline.es2
package eventstore

import scala.collection._

trait EventStore {
  type EventStoreListener = CommittedEvent => Unit
  def commit(events: Iterable[UncommittedEvent])
  def load(source: Identifier): Iterable[CommittedEvent]
  def addListener(callback: EventStoreListener)
}

class MemoryEventStore extends EventStore {

  def commit(events: Iterable[UncommittedEvent]) {
    if (events.isEmpty) return

    val committed = for (event <- events) yield Committed(event.source, event.payload)
    committed foreach { c => storedEvents.getOrElseUpdate(c.source, mutable.Queue()) += c };
    for (listener <- listeners; c <- committed) listener(c)
  }

  def load(source: Identifier): Iterable[CommittedEvent] = storedEvents.getOrElse(source, Iterable.empty)

  def addListener(callback: EventStoreListener) {
    listeners += callback;
  }

  private val storedEvents: mutable.Map[Identifier, mutable.Queue[CommittedEvent]] = mutable.Map()
  private val listeners: mutable.Queue[EventStoreListener] = mutable.Queue()
}
