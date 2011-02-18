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

    SessionFactory.concreteFactory = Some(()=>
      Session.create(
        java.sql.DriverManager.getConnection(jdbcUrl),
        new H2Adapter))


    try {
      transaction {
        Session.currentSession.setLogger(println)
        SquerylEventStore.create
      }
    } catch {
      case exception => println(exception)
    }
  }

  def insertAndSelect {
    transaction {
      Session.currentSession.setLogger(println)
      implicit val formats = DefaultFormats
      val example = ExampleEvent("example")
      val json = write(example)
      val inserted = SquerylEventStore.events.insert(EventRecord(123, newIdentifier.toString, json))
      val e: Iterable[EventRecord] = from(SquerylEventStore.events)(event => select(event))
      for (ev <- e) {
        println(ev)
        println(read[ExampleEvent](ev.event))
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
