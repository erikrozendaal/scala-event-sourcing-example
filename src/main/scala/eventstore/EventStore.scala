package com.zilverline.es2.eventstore

import java.util.UUID
import scala.collection._

class EventStore {
  type EventListener = (UUID, AnyRef) => Unit

  def save(source: UUID, event: AnyRef) {
    storedEvents.getOrElseUpdate(source, mutable.Queue()) += event;
    listeners foreach {callback => callback(source, event)}
  }

  def load(source: UUID): Iterable[AnyRef] = storedEvents.getOrElse(source, mutable.Queue())

  def addListener(callback: EventListener) {
    listeners += callback;
  }

  private val storedEvents: mutable.Map[UUID, mutable.Queue[AnyRef]] = mutable.Map()
  private val listeners: mutable.Queue[EventListener] = mutable.Queue()
}
