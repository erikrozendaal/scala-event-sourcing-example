package com.zilverline.es2
package eventstore

import java.util.UUID
import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

case class EventRecord(id: Long, source: String, event: String) extends KeyedEntity[Long] {
  def this() = this(0, "", "")
}

object SquerylEventStore extends Schema {
  val EventRecords = table[EventRecord]("event_records")

  on(EventRecords)(t => declare(
    t.source is(indexed, dbType("varchar(36)")),
    t.event is(dbType("varchar"))))
}

class SquerylEventStore(serializer: Serializer) extends EventStore {
  import SquerylEventStore.EventRecords

  private implicit def stringToIdentifier(s: String): Identifier = UUID.fromString(s)
  private implicit def identifierToString(identifier: Identifier): String = identifier.toString

  def commit(events: Iterable[UncommittedEvent]) {
    if (events.isEmpty) return

    transaction {
      val records = events map (uncommitted => EventRecord(0, uncommitted.source, write(uncommitted.event)))
      EventRecords.insert(records)
    }
    val committed = for (event <- events) yield Committed(event.source, event.event)
    for (listener <- listeners; c <- committed) listener(c)
  }

  def load(source: Identifier): Iterable[CommittedEvent] = transaction {
    val records = from(EventRecords)(r => where(r.source === (source: String)).select(r))
    for (record <- records) yield Committed(record.source, read(record.event))
  }

  def addListener(callback: EventStoreListener) {
    listeners = callback :: listeners
  }

  def replayAllEvents {
    transaction {
      val records = from(EventRecords)(r => select(r).orderBy(r.id asc))
      for (record <- records; listener <- listeners) {
        listener(Committed(record.source, read(record.event)))
      }
    }
  }

  private var listeners: List[EventStoreListener] = Nil

  private def write = serializer.serialize _
  private def read = serializer.deserialize _
}
