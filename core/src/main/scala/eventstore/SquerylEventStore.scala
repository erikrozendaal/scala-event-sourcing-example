package com.zilverline.es2
package eventstore

import java.util.UUID
import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

case class EventStream(source: String, revision: Long) {
  def this() = this ("", 0L)
}

case class EventStreamRecord(id: Long, source: String, sequence: Long, event: String) extends KeyedEntity[Long] {
  def this() = this (0, "", 0L, "")
}

object SquerylEventStore extends Schema {
  val EventStreams = table[EventStream]
  val EventStreamRecords = table[EventStreamRecord]

  on(EventStreams) {t =>
    declare(
      t.source is (primaryKey, dbType("varchar(36)")))
  }
  on(EventStreamRecords) {t =>
    declare(
      t.source is (dbType("varchar(36)")),
      t.event is (dbType("varchar")),
      columns(t.source, t.sequence) are (unique))
  }
}

class SquerylEventStore(serializer: Serializer) extends EventStore {

  import SquerylEventStore._

  private implicit def stringToIdentifier(s: String): Identifier = UUID.fromString(s)

  private implicit def identifierToString(identifier: Identifier): String = identifier.toString

  def commit(events: Iterable[UncommittedEvent]) {
    if (events.isEmpty) return
    verifyEventsBeforeCommit(events)

    synchronized {
      insertEvents(events)
      dispatchEvents(events map (event => Committed(event.source, event.sequence, event.payload)))
    }
  }

  def load(source: Identifier): Iterable[CommittedEvent] = transaction {
    val records = from(EventStreamRecords)(r =>
      where(r.source === (source: String))
        .select(r)
        .orderBy(r.sequence asc))
    records.map(record => Committed(record.source, record.sequence, read(record.event)))
  }

  def replayAllEvents {
    transaction {
      val records = from(EventStreamRecords)(r => select(r).orderBy(r.id asc))
      val events = records.toStream.map(record => Committed(record.source, record.sequence, read(record.event)))
      dispatchEvents(events)
    }
  }

  private def write = serializer.serialize _

  private def read = serializer.deserialize _

  private def createEventStream(first: UncommittedEvent): Any = {
    try {
      EventStreams.insert(EventStream(first.source, first.sequence))
    } catch {
      case exception =>
        val message = exception.getMessage.toLowerCase
        if (message.contains("unique") || message.contains("primary"))
          throw new OptimisticLockingException("event stream already exists for event <" + first + ">")
        else
          throw exception
    }
  }

  private def updateEventStream(first: UncommittedEvent): Unit = {
    val count = update(EventStreams)(r =>
      where(r.source === (first.source: String) and r.revision === first.sequence - 1)
        .set(r.revision := first.sequence))
    if (count == 0)
      throw new OptimisticLockingException("sequence number <" + first.sequence + "> does not match expected")
  }

  private def insertEvents(events: Iterable[UncommittedEvent]) {
    val first = events.head
    transaction {
      if (first.sequence == InitialRevision) {
        createEventStream(first)
      } else {
        updateEventStream(first)
      }
      val records = events.map(event => EventStreamRecord(0, event.source, event.sequence, write(event.payload)))
      EventStreamRecords.insert(records)
    }
  }
}
