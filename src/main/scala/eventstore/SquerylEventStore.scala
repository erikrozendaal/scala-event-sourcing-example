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

  private def write = serializer.serialize _
  private def read = serializer.deserialize _

  implicit def stringToIdentifier(s: String): Identifier = UUID.fromString(s)
  implicit def identifierToString(identifier: Identifier): String = identifier.toString

  def commit(events: Iterable[UncommittedEvent]) {
    transaction {
      val records = events map (uncommitted => EventRecord(0, uncommitted.source, write(uncommitted.event)))
      EventRecords.insert(records)
    }
    for {listener <- listeners; uncommitted <- events} {
      listener(Committed(uncommitted.source, uncommitted.event))
    }
  }

  def load(source: Identifier): Iterable[CommittedEvent] = transaction {
    val records = from(EventRecords)(r => where(r.source === (source: String)).select(r))
    for (record <- records) yield Committed(record.source, read(record.event))
  }

  def addListener(callback: EventStoreListener) {
    listeners = callback :: listeners
  }

  private var listeners: List[EventStoreListener] = Nil
}
