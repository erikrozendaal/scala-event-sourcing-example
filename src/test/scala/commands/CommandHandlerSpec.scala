package com.zilverline.es2
package commands

import events._

object CommandHandlerSpec extends org.specs.Specification {

  import CommandHandler._

  val AggregateIdentifier = newIdentifier

  def subject = handler {
    command: ExampleCommand =>
      save(AggregateIdentifier, ExampleEvent("content"))
  }

  "command handler" should {
    "be able to save events" in {
      val result = subject(ExampleCommand("hello"))(UnitOfWork.empty)

      result must beEqualTo(Accepted(AggregateIdentifier, ExampleEvent("content")))
    }
  }
}
