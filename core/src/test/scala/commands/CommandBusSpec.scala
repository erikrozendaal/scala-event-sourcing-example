package com.zilverline.es2
package commands

import transaction._

class CommandBusSpec extends org.specs.Specification {

  val Source = newIdentifier

  val eventStore = new eventstore.MemoryEventStore
  val subject = new CommandBus(eventStore)

  "command bus without handlers" should {
    "fail when no handler found for command" in {
      subject.send(ExampleCommand("hello")) must throwA[IllegalArgumentException]
    }
  }

  "command bus with handlers" should {
    var handlerInvoked = false

    def testHandler = CommandHandler[ExampleCommand] {command => handlerInvoked = true; transaction.pure()}
    subject.registerHandler(testHandler)

    "invoke handler based on command type" in {
      subject.send(ExampleCommand("example"))

      handlerInvoked must beTrue
    }

    "fail when no handler found for command" in {
      subject.send(AnotherCommand("hello")) must throwA[IllegalArgumentException]
    }
  }

  "command bus" should {
    subject register {
      command: ExampleCommand =>
        modifyEventSource(Source, ExampleEvent(command.content))(_ => None) andThen transaction.pure()
    }
    subject register {
      command: AnotherCommand =>
        modifyEventSource(Source, ExampleEvent(command.content))(_ => None) andThen transaction.rollback
    }

    "commit accepted unit of work" in {
      subject.send(ExampleCommand("hello"))

      eventStore.load(Source) must contain(Committed(Source, 1, ExampleEvent("hello")))
    }

    "rollback rejected unit of work" in {
      subject.send(AnotherCommand("hello"))

      eventStore.load(Source) must beEmpty
    }

  }
}
