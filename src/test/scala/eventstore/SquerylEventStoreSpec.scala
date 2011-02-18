package com.zilverline.es2
package eventstore

import org.squeryl._
import org.squeryl.adapters._
import org.squeryl.PrimitiveTypeMode._
import net.liftweb.json.Serialization.{read, write}

import net.liftweb.json._

object Test {
  def initialize {
    val jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    val jdbcDriver = "org.h2.Driver"

    Class.forName(jdbcDriver)

    SessionFactory.concreteFactory = Some {
      () =>
        val result = Session.create(java.sql.DriverManager.getConnection(jdbcUrl), new H2Adapter)
        result.setLogger(println)
        result
    }

    transaction {
      SquerylEventStore.create
    }
  }

  def insertAndSelect {
    transaction {
      implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[ExampleEvent])))
      val example = ExampleEvent("example")
      val json = write(example)
      val inserted = SquerylEventStore.EventRecords.insert(EventRecord(123, newIdentifier.toString, json))
      val records = from(SquerylEventStore.EventRecords)(select(_))
      for (ev <- records) {
        println(ev)
        println(read[DomainEvent](ev.event))
      }
    }
  }
}

object SquerylEventStoreSpec extends org.specs.Specification {

  doBeforeSpec {
    Test.initialize
  }

  "SquerylEventStore" should {
    "create event table when connecting" in {
      Test.insertAndSelect
    }
  }

}
