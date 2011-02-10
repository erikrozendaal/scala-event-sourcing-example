package com.zilverline.es2.eventstore

import java.util.UUID
import scala.collection._

class EventStore {
  def save(source: UUID, event: AnyRef) {
    storedEvents.getOrElseUpdate(source, mutable.Queue()) += event;
  }

  def load(source: UUID): Iterable[AnyRef] = storedEvents.getOrElse(source, mutable.Queue())

  private val storedEvents: mutable.Map[UUID, mutable.Queue[AnyRef]] = mutable.Map()
}
