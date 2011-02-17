package com.zilverline.es2
package eventstore

import scala.collection._

class EventStore {
  type EventStoreListener = Committed[DomainEvent] => Unit

  def commit(events: Iterable[Uncommitted[DomainEvent]]) {
    for (event <- events) {
      val committed = Committed(event.source, event.event)
      storedEvents.getOrElseUpdate(event.source, mutable.Queue()) += committed;
      listeners foreach {callback => callback(committed)}
    }
  }

  def load(source: Identifier): Iterable[Committed[DomainEvent]] = storedEvents.getOrElse(source, Iterable.empty)

  def addListener(callback: EventStoreListener) {
    listeners += callback;
  }

  private val storedEvents: mutable.Map[Identifier, mutable.Queue[Committed[DomainEvent]]] = mutable.Map()
  private val listeners: mutable.Queue[EventStoreListener] = mutable.Queue()
}
