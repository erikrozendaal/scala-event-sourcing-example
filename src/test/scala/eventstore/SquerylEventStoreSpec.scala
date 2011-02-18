package com.zilverline.es2
package eventstore

import org.squeryl._
import org.squeryl.adapters._
import org.squeryl.PrimitiveTypeMode._

import net.liftweb.json._

object Test {
  def initialize {
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
}

object SquerylEventStoreSpec extends EventStoreSpec {

  val serializer = new JsonSerializer()(Serialization.formats(new ReflectionTypeHints))
  val subject = new SquerylEventStore(serializer)

  doBeforeSpec {
    Test.initialize
  }

  val Source = newIdentifier

  "SquerylEventStore" should {

    forExample("commit and load events") in {
      val originals = Uncommitted(Source, ExampleEvent("example")) :: Uncommitted(Source, AnotherEvent("another")) :: Nil

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

      subject.commit(Seq(Uncommitted(Source, ExampleEvent("example"))))

      committed must beEqualTo(Some(Committed(Source, ExampleEvent("example"))))
    }
  }

}
