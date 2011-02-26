package com.zilverline.es2
package commands

import eventstore.MemoryEventStore

class CommandHandlerSpec extends org.specs.Specification {

  val AggregateIdentifier = newIdentifier

  "commit command handler" should {
    shareVariables()

    val eventStore = new MemoryEventStore
    val commands = new CommandBus(eventStore)
    commands register CommandHandler.commitCommandHandler

    "commit event to new event source" in {
      commands.send(Commit(AggregateIdentifier, 0, ExampleEvent("initial")))

      eventStore.load(AggregateIdentifier) must beEqualTo(Seq(Committed(AggregateIdentifier, 1, ExampleEvent("initial"))))
    }

    "append events to existing event source" in {
      commands.send(Commit(AggregateIdentifier, 1, ExampleEvent("initial")))
    }
  }
}
