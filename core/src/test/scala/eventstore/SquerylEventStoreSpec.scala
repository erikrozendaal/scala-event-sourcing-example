package com.zilverline.es2
package eventstore

import net.liftweb.json._
import org.squeryl._
import org.squeryl.adapters._
import org.squeryl.PrimitiveTypeMode._
import scala.collection.mutable.ArrayBuffer

object TestDatabase {
  lazy val initialize = {
    val jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    val jdbcDriver = "org.h2.Driver"

    Class.forName(jdbcDriver)

    SessionFactory.concreteFactory = Some {
      () =>
        val result = Session.create(java.sql.DriverManager.getConnection(jdbcUrl), new H2Adapter)
        // result.setLogger(println)
        result
    }

    transaction {
      SquerylEventStore.create
    }
  }

  def clear = transaction {
    SquerylEventStore.EventStreamRecords.delete(from(SquerylEventStore.EventStreamRecords)(select(_)))
    SquerylEventStore.EventStreams.delete(from(SquerylEventStore.EventStreams)(select(_)))
  }
}

class SquerylEventStoreSpec extends EventStoreSpec {

  val serializer = new JsonSerializer()(Serialization.formats(new ReflectionTypeHints))
  val subject = new SquerylEventStore(serializer)

  TestDatabase.initialize

  val Source = newIdentifier

  "SquerylEventStore" should {
    doBefore {TestDatabase.clear}

    val originals = ExampleEvent("example") :: AnotherEvent("another") :: Nil

    forExample("commit and load events") in {
      subject.commit(Commit(Source, 0, originals))

      subject.load(Source) must beEqualTo(Seq(
        Committed(Source, 1, ExampleEvent("example")),
        Committed(Source, 2, AnotherEvent("another"))))
    }

    forExample("invoke listeners for each committed event") in {
      var committed: Option[CommittedEvent] = None
      subject addListener {
        c => committed = Some(c)
      }

      subject.commit(Commit(Source, 0, originals.take(1)))

      committed must beEqualTo(Some(Committed(Source, 1, ExampleEvent("example"))))
    }

    forExample("replay previously committed events") in {
      var replayed: ArrayBuffer[CommittedEvent] = ArrayBuffer.empty
      subject.commit(Commit(Source, 0, originals))

      subject addListener (replayed += _)
      subject.replayAllEvents

      replayed must beEqualTo(Seq(
        Committed(Source, 1, ExampleEvent("example")),
        Committed(Source, 2, AnotherEvent("another"))))
    }
  }

}
