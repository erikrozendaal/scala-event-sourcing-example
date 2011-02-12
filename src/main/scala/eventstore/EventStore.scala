package com.zilverline.es2

import java.util.UUID
import scala.collection._

package eventstore {

  import eventing._

  class EventStore {
    type EventListener = (EventSourceIdentifier, Event) => Unit

    def save(event: Event) {
      storedEvents.getOrElseUpdate(event.source, mutable.Queue()) += event;
      listeners foreach {callback => callback(event.source, event)}
    }

    def load(source: UUID): Iterable[Event] = storedEvents.getOrElse(source, Iterable.empty)

    def addListener(callback: EventListener) {
      listeners += callback;
    }

    private val storedEvents: mutable.Map[UUID, mutable.Queue[Event]] = mutable.Map()
    private val listeners: mutable.Queue[EventListener] = mutable.Queue()
  }

}
