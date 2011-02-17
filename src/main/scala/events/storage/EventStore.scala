package com.zilverline.es2
package events.storage

import scala.collection._

import events._

class EventStore {
  type EventStoreListener = CommittedEvent[DomainEvent] => Unit

  def commit(events: Iterable[UncommittedEvent[DomainEvent]]) {
    for (event <- events) {
      val committed = CommittedEvent(event.source, event.payload)
      storedEvents.getOrElseUpdate(event.source, mutable.Queue()) += committed;
      listeners foreach {callback => callback(committed)}
    }
  }

  def load(source: Identifier): Iterable[CommittedEvent[DomainEvent]] = storedEvents.getOrElse(source, Iterable.empty)

  def addListener(callback: EventStoreListener) {
    listeners += callback;
  }

  private val storedEvents: mutable.Map[Identifier, mutable.Queue[CommittedEvent[DomainEvent]]] = mutable.Map()
  private val listeners: mutable.Queue[EventStoreListener] = mutable.Queue()
}
