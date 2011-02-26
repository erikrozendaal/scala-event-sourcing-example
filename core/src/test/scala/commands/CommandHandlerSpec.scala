package com.zilverline.es2
package commands

import eventstore.MemoryEventStore

class CommandHandlerSpec extends org.specs.Specification {

  val AggregateIdentifier = newIdentifier

  "commit command handler" should {
    shareVariables()

    val eventStore = new MemoryEventStore
    val commands = new CommandBus(eventStore)
    commands register CommandHandler.updateCommandHandler

    "commit event to new event source" in {
      commands.send(Update(AggregateIdentifier, InitialRevision, ExampleEvent("initial")))

      eventStore.load(AggregateIdentifier) must beEqualTo(Seq(Committed(AggregateIdentifier, 1, ExampleEvent("initial"))))
    }

    "append events to existing event source" in {
      commands.send(Update(AggregateIdentifier, InitialRevision + 1, ExampleEvent("additional")))

      eventStore.load(AggregateIdentifier) must beEqualTo(Seq(
        Committed(AggregateIdentifier, 1, ExampleEvent("initial")),
        Committed(AggregateIdentifier, 2, ExampleEvent("additional"))))
    }
  }
}
