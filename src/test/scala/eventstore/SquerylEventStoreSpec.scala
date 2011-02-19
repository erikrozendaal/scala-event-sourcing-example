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
    SquerylEventStore.EventRecords.delete(from(SquerylEventStore.EventRecords)(select(_)))
  }
}

object SquerylEventStoreSpec extends EventStoreSpec {

  val serializer = new JsonSerializer()(Serialization.formats(new ReflectionTypeHints))
  val subject = new SquerylEventStore(serializer)

  TestDatabase.initialize

  val Source = newIdentifier

  "SquerylEventStore" should {
    doBefore {TestDatabase.clear}

    val originals = Uncommitted(Source, ExampleEvent("example")) :: Uncommitted(Source, AnotherEvent("another")) :: Nil

    forExample("commit and load events") in {
      subject.commit(originals)

      subject.load(Source) must beEqualTo(Seq(
        Committed(Source, ExampleEvent("example")),
        Committed(Source, AnotherEvent("another"))))
    }

    forExample("invoke listeners for committed events") in {
      var committed: Option[CommittedEvent] = None
      subject addListener {
        c => committed = Some(c)
      }

      subject.commit(originals.take(1))

      committed must beEqualTo(Some(Committed(Source, ExampleEvent("example"))))
    }

    forExample("replay previously committed events") in {
      var replayed: ArrayBuffer[CommittedEvent] = ArrayBuffer.empty
      subject.commit(originals)

      subject addListener (replayed += _)
      subject.replayAllEvents

      replayed must beEqualTo(Seq(
        Committed(Source, ExampleEvent("example")),
        Committed(Source, AnotherEvent("another"))))
    }
  }

}
