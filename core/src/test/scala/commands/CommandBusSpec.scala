package com.zilverline.es2
package commands

import behavior._

class CommandBusSpec extends org.specs2.mutable.SpecificationWithJUnit {

  "command bus" should {
    "invoke handler based on command type" in context().invokeHandler
    "fail when no handler found for command" in context().failWithoutHandler
    "commit accepted unit of work" in context().commitEvents
  }

  case class context() {
    val Source = newIdentifier

    val eventStore = new eventstore.MemoryEventStore
    val subject = new CommandBus(eventStore)

    var handlerInvoked = false

    def testHandler = CommandHandler[ExampleCommand] {command => handlerInvoked = true}

    subject.registerHandler(testHandler)

    def invokeHandler = {
      subject.send(ExampleCommand("example"))

      handlerInvoked must beTrue
    }

    def failWithoutHandler = {
      subject.send(AnotherCommand("hello")) must throwA[IllegalArgumentException]

      handlerInvoked must beFalse
    }

    def commitEvents = {
      subject.register[ExampleCommand] {command =>
        Behavior.modifyEventSource(Source, ExampleEvent(command.content))(_ => None)
      }

      subject.send(ExampleCommand("hello"))

      eventStore.load(Source) must contain(Committed(Source, 1, ExampleEvent("hello")))
    }
  }
}
