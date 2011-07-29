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
      () => Session.create(java.sql.DriverManager.getConnection(jdbcUrl), new H2Adapter)
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

@org.junit.runner.RunWith(classOf[org.specs2.runner.JUnitRunner])
class SquerylEventStoreSpec extends EventStoreSpec {

  sequential

  TestDatabase.initialize

  def serializer = new JsonSerializer()(Serialization.formats(new FullTypeHints(classOf[DomainEvent] :: Nil)))
  
  override def makeEmptyEventStore(dispatcher: Seq[CommittedEvent] => Unit) = {
    TestDatabase.clear
    new SquerylEventStore(serializer, dispatcher)
  }

  val Source = newIdentifier

  "SquerylEventStore" should {
    val originals = ExampleEvent("example") :: AnotherEvent("another") :: Nil

    forExample("commit and load events") ! new Context {
      subject.commit(Commit(Source, 0, originals))

      subject.load(Source) must beEqualTo(Seq(
        Committed(Source, 1, ExampleEvent("example")),
        Committed(Source, 2, AnotherEvent("another"))))
    }

    forExample("invoke listeners for each committed event") ! new Context {
      var committed = Vector.empty[CommittedEvent]
      override val subject = makeEmptyEventStore(committed ++= _)

      subject.commit(Commit(Source, 0, originals.take(1)))

      committed must beEqualTo(Vector(Committed(Source, 1, ExampleEvent("example"))))
    }

    forExample("replay previously committed events") ! new Context {
      var replayed = Vector.empty[CommittedEvent]
      override val subject = makeEmptyEventStore(replayed ++= _)
      subject.commit(Commit(Source, 0, originals))
      replayed = Vector.empty[CommittedEvent]

      subject.replayAllEvents

      replayed must beEqualTo(Seq(
        Committed(Source, 1, ExampleEvent("example")),
        Committed(Source, 2, AnotherEvent("another"))))
    }
  }
}
