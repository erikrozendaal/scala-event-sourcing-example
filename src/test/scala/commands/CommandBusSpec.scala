package com.zilverline.es2.commands

import org.specs.Specification

object CommandBusSpec extends Specification {

  private case class ExampleCommand(content: String)

  "command bus" should {
    val subject = new CommandBus

    "fail when no handler found for command" in {
      subject.send(ExampleCommand("hello")) must throwA[IllegalArgumentException]
    }
  }

}
