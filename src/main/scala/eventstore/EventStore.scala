package com.zilverline.es2

import scala.collection._

package eventstore {
  import eventing._

  class EventStore {
    type EventStoreListener = Message => Unit

    def save(source: EventSourceIdentifier, event: Event) {
      storedEvents.getOrElseUpdate(source, mutable.Queue()) += event;
      listeners foreach {callback => callback(Message(source, event))}
    }

    def load(source: EventSourceIdentifier): Iterable[Event] = storedEvents.getOrElse(source, Iterable.empty)

    def addListener(callback: EventStoreListener) {
      listeners += callback;
    }

    private val storedEvents: mutable.Map[EventSourceIdentifier, mutable.Queue[Event]] = mutable.Map()
    private val listeners: mutable.Queue[EventStoreListener] = mutable.Queue()
  }

}
