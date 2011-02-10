package com.zilverline.es2.eventstore

import java.util.UUID
import scala.collection._

class EventStore {
  type EventListener = (UUID, AnyRef) => Unit

  def save(source: UUID, event: AnyRef) {
    storedEvents.getOrElseUpdate(source, mutable.Queue()) += event;
    listener foreach {callback => callback(source, event)}
  }

  def load(source: UUID): Iterable[AnyRef] = storedEvents.getOrElse(source, mutable.Queue())

  def listen(callback: EventListener) {
    listener = Some(callback);
  }

  private val storedEvents: mutable.Map[UUID, mutable.Queue[AnyRef]] = mutable.Map()
  private var listener: Option[EventListener] = None
}
