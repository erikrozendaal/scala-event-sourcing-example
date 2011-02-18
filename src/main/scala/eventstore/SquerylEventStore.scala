package com.zilverline.es2
package eventstore

import java.util.UUID
import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

case class EventRecord(id: Long, aggregate: String, event: String) extends KeyedEntity[Long] {
  def this() = this(0, "", "")
}

object SquerylEventStore extends Schema {
  val EventRecords = table[EventRecord]("event_records")

  on(EventRecords)(t => declare(t.event is(dbType("varchar"))))
}

class SquerylEventStore(serializer: Serializer) extends EventStore {
  import SquerylEventStore.EventRecords

  private def write = serializer.serialize _
  private def read = serializer.deserialize _

  def commit(events: Iterable[UncommittedEvent]) {
    transaction {
      for (uncommitted <- events) {
        EventRecords.insert(EventRecord(0, uncommitted.source.toString, write(uncommitted.event)))
      }
    }
    for {listener <- listeners; uncommitted <- events} {
      listener(Committed(uncommitted.source, uncommitted.event))
    }
  }

  def load(source: Identifier): Iterable[CommittedEvent] = transaction {
    val records = from(EventRecords)(r => where(r.aggregate === source.toString).select(r))
    for (record <- records) yield Committed(UUID.fromString(record.aggregate), read(record.event))
  }

  def addListener(callback: EventStoreListener) {
    listeners = callback :: listeners
  }

  private var listeners: List[EventStoreListener] = Nil
}
