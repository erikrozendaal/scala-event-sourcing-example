package com.zilverline.es2
package events.storage

import scala.collection._

import events._

class EventStore {
  type EventStoreListener = CommittedEvent => Unit

  def commit(source: Identifier, event: Event) {
    storedEvents.getOrElseUpdate(source, mutable.Queue()) += event;
    listeners foreach {callback => callback(CommittedEvent(source, event))}
  }

  def load(source: Identifier): Iterable[Event] = storedEvents.getOrElse(source, Iterable.empty)

  def addListener(callback: EventStoreListener) {
    listeners += callback;
  }

  private val storedEvents: mutable.Map[Identifier, mutable.Queue[Event]] = mutable.Map()
  private val listeners: mutable.Queue[EventStoreListener] = mutable.Queue()
}
