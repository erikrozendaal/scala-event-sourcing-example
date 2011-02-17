package com.zilverline.es2
package commands

object CommandHandlerSpec extends org.specs.Specification {

  import behavior._
  import CommandHandler._

  val AggregateIdentifier = newIdentifier

  def subject = CommandHandler {
    command: ExampleCommand =>
      record(AggregateIdentifier, ExampleEvent("content"))
  }

  "command handler" should {
    "be able to save events" in {
      val result = subject(ExampleCommand("hello"))(UnitOfWork(Nil, null))

      result must beEqualTo(Accepted(UnitOfWork(List(Uncommitted(AggregateIdentifier, ExampleEvent("content"))), null), Uncommitted(AggregateIdentifier, ExampleEvent("content"))))
    }
  }
}
