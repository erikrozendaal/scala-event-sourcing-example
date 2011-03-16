package com.zilverline.es2
package commands

import transaction._
import org.specs2.execute.Success

class CommandBusSpec extends org.specs2.mutable.SpecificationWithJUnit {

  trait Context extends Success {
    val Source = newIdentifier

    val eventStore = new eventstore.MemoryEventStore
    val subject = new CommandBus(eventStore)

    var handlerInvoked = false

    def testHandler = CommandHandler[ExampleCommand] {command => handlerInvoked = true; transaction.pure()}

    subject.registerHandler(testHandler)
  }

  "command bus" should {
    "invoke handler based on command type" in new Context {
      subject.send(ExampleCommand("example"))

      handlerInvoked must beTrue
    }

    "fail when no handler found for command" in new Context {
      subject.send(AnotherCommand("hello")) must throwA[IllegalArgumentException]

      handlerInvoked must beFalse
    }

    "commit accepted unit of work" in new Context {
      subject.register[ExampleCommand] {command =>
        modifyEventSource(Source, ExampleEvent(command.content))(_ => None) andThen transaction.pure()
      }

      subject.send(ExampleCommand("hello"))

      eventStore.load(Source) must contain(Committed(Source, 1, ExampleEvent("hello")))
    }

    "rollback rejected unit of work" in new Context {
      subject.register[AnotherCommand] {command =>
        modifyEventSource(Source, ExampleEvent(command.content))(_ => None) andThen transaction.rollback
      }

      subject.send(AnotherCommand("hello"))

      eventStore.load(Source) must beEmpty
    }
  }
}
