package com.zilverline.es2
package examples

import org.specs.Specification

import reports._

case class InstructionAdded(text: String)

case class InstructionIndex(instructions: List[String] = List.empty) extends Index {
  def applyEvent = {
    case Event(event: InstructionAdded) => copy(event.text :: instructions)
  }
}

object InstructionsSpec extends Specification {

  val Source = newIdentifier

  val eventStore = new eventstore.EventStore
  val indexes = new Indexes

  indexes.add(InstructionIndex())

  eventStore.addListener(commit => indexes.process(commit))

  "instruction" should {
    "show up in index when added" in {
      eventStore.commit(Iterable(Uncommitted(Source, InstructionAdded("hello"))))

      indexes.get[InstructionIndex].instructions must contain("hello")
    }
  }
}
