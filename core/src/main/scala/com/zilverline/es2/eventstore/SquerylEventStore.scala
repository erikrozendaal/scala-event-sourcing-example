package com.zilverline.es2
package eventstore

import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

case class EventStream(source: String, revision: Long) {
  def this() = this ("", 0L)
}

case class EventStreamRecord(id: Long, source: String, sequence: Long, event: Array[Byte]) extends KeyedEntity[Long] {
  def this() = this (0, "", 0L, Array.empty)
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
      columns(t.source, t.sequence) are (unique))
  }
}

class SquerylEventStore(serializer: Serializer, dispatcher: Seq[CommittedEvent] => Unit) extends EventStore {

  import SquerylEventStore._

  private implicit def stringToIdentifier(s: String): Identifier = Identifier.fromString(s)

  private implicit def identifierToString(identifier: Identifier): String = identifier.toString

  def commit(attempt: Commit) {
    if (attempt.events.isEmpty) return

    this synchronized {
      // TODO only lock the event source being committed
      val committed = makeCommittedEvents(attempt)
      insertEvents(attempt, committed)
      dispatcher(committed)
    }
  }

  def load(source: Identifier): Seq[CommittedEvent] = transaction {
    val records = from(EventStreamRecords)(r =>
      where(r.source === (source: String))
        .select(r)
        .orderBy(r.sequence asc))
    records.map(record => Committed(record.source, record.sequence, read(record.event))).toSeq
  }

  override def replayAllEvents() {
    transaction {
      val records = from(EventStreamRecords)(r => select(r).orderBy(r.id asc))
      val events = records.toStream.map(record => Committed(record.source, record.sequence, read(record.event)))
      dispatcher(events)
    }
  }

  private def write = serializer.serialize _

  private def read(s: Array[Byte]) = try {
    serializer.deserialize(s)
  } catch {
    case e => println("failed to deserialize " + s); throw e
  }

  private def createEventStream(attempt: Commit) {
    try {
      EventStreams.insert(EventStream(attempt.source, attempt.events.size))
    } catch {
      case exception =>
        val message = exception.getMessage.toLowerCase
        if (message.contains("unique") || message.contains("primary"))
          throw new OptimisticLockingException("event stream already exists for event source <" + attempt.source + ">")
        else
          throw exception
    }
  }

  private def updateEventStream(attempt: Commit) {
    val count = update(EventStreams)(r =>
      where(r.source === (attempt.source: String) and r.revision === attempt.revision)
        .set(r.revision := r.revision.~ + attempt.events.size))
    if (count == 0)
      throw new OptimisticLockingException("sequence number <" + attempt.revision + "> does not match expected for event source <" + attempt.source + ">")
  }

  private def insertEvents(attempt: Commit, events: Iterable[CommittedEvent]) {
    inTransaction {
      if (attempt.revision == 0) {
        createEventStream(attempt)
      } else {
        updateEventStream(attempt)
      }
      val records = events.map(event => EventStreamRecord(0, event.eventSourceId, event.sequence, write(event.payload)))
      EventStreamRecords.insert(records)
    }
  }
}
