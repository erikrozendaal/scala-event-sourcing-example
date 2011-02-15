package com.zilverline.es2
package commands

object CommandBusSpec extends org.specs.Specification {

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

    def testHandler = handler {command: ExampleCommand => handlerInvoked = true; CommandHandler.accept}
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
    subject.register(handler {
      command: ExampleCommand =>
        save(Source, ExampleEvent(command.content)) andThen CommandHandler.accept
    })
    subject.register(handler {
      command: AnotherCommand =>
        save(Source, ExampleEvent(command.content)) andThen CommandHandler.reject
    })

    "commit accepted unit of work" in {
      subject.send(ExampleCommand("hello"))

      eventStore.load(Source) must contain(ExampleEvent("hello"))
    }

    "rollback rejected unit of work" in {
      subject.send(AnotherCommand("hello"))

      eventStore.load(Source) must beEmpty
    }

  }
}
