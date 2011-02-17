package com.zilverline.es2
package eventstore

import scala.collection._

class EventStore {
  type EventStoreListener = CommittedEvent => Unit

  def commit(events: Iterable[UncommittedEvent]) {
    for (event <- events) {
      val committed = Committed(event.source, event.event)
      storedEvents.getOrElseUpdate(event.source, mutable.Queue()) += committed;
      listeners foreach {callback => callback(committed)}
    }
  }

  def load(source: Identifier): Iterable[CommittedEvent] = storedEvents.getOrElse(source, Iterable.empty)

  def addListener(callback: EventStoreListener) {
    listeners += callback;
  }

  private val storedEvents: mutable.Map[Identifier, mutable.Queue[CommittedEvent]] = mutable.Map()
  private val listeners: mutable.Queue[EventStoreListener] = mutable.Queue()
}
