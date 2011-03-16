package com.zilverline.es2
package commands

import eventstore.MemoryEventStore
import org.specs2.execute.Success

class CommandHandlerSpec extends org.specs2.mutable.SpecificationWithJUnit {

  trait Context extends Success {
    val AggregateIdentifier = newIdentifier

    val eventStore = new MemoryEventStore
    val commands = new CommandBus(eventStore)

    commands registerHandler CommandHandler.updateCommandHandler
  }

  "update command handler" should {
    "commit event to new event source" in new Context {
      commands.send(Update(AggregateIdentifier, InitialRevision, ExampleEvent("initial")))

      eventStore.load(AggregateIdentifier) must beEqualTo(Seq(Committed(AggregateIdentifier, 1, ExampleEvent("initial"))))
    }

    "append events to existing event source" in new Context {
      commands.send(Update(AggregateIdentifier, InitialRevision, ExampleEvent("initial")))
      commands.send(Update(AggregateIdentifier, InitialRevision + 1, ExampleEvent("additional")))

      eventStore.load(AggregateIdentifier) must beEqualTo(Seq(
        Committed(AggregateIdentifier, 1, ExampleEvent("initial")),
        Committed(AggregateIdentifier, 2, ExampleEvent("additional"))))
    }
  }
}
