package com.zilverline.es2
package commands

import domain._, Behavior._

@org.junit.runner.RunWith(classOf[org.specs2.runner.JUnitRunner])
class CommandsSpec extends org.specs2.mutable.Specification {

  "command bus" should {
    "commit events from reaction" in context().commitEvents
  }

  case class context() {
    val Source = newIdentifier

    implicit val aggregates = new Aggregates()
    val eventStore = new eventstore.MemoryEventStore(events => ())
    val subject = new Commands(eventStore, aggregates)

    def commitEvents = {
      subject.create(Source) {
        record(ExampleEvent("hello"))(_ => None)
      }

      eventStore.load(Source) must contain(Committed(Source, 1, ExampleEvent("hello")))
    }
  }
}
