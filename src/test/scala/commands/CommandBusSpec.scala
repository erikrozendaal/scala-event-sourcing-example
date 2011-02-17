package com.zilverline.es2
package commands

object CommandBusSpec extends org.specs.Specification {

  import domain._
  import domain.Behaviors._
  import events._
  import events.storage._
  import CommandHandler._

  val Source = newIdentifier

  val eventStore = new EventStore
  val subject = new CommandBus(eventStore)

  "command bus without handlers" should {
    "fail when no handler found for command" in {
      subject.send(ExampleCommand("hello")) must throwA[IllegalArgumentException]
    }
  }

  "command bus with handlers" should {
    var handlerInvoked = false

    def testHandler = CommandHandler {command: ExampleCommand => handlerInvoked = true; Behaviors.accept()}
    subject.register(testHandler)

    "invoke handler based on command type" in {
      subject.send(ExampleCommand("example"))

      handlerInvoked must beTrue
    }

    "fail when no handler found for command" in {
      subject.send(AnotherCommand("hello")) must throwA[IllegalArgumentException]
    }
  }

  "command bus" should {
    subject.register(CommandHandler {
      command: ExampleCommand =>
        record(Source, ExampleEvent(command.content)) andThen Behaviors.accept()
    })
    subject.register(CommandHandler {
      command: AnotherCommand =>
        record(Source, ExampleEvent(command.content)) andThen Behaviors.reject("failed")
    })

    "commit accepted unit of work" in {
      subject.send(ExampleCommand("hello"))

      eventStore.load(Source) must contain(Committed(Source, ExampleEvent("hello")))
    }

    "rollback rejected unit of work" in {
      subject.send(AnotherCommand("hello"))

      eventStore.load(Source) must beEmpty
    }

  }
}
