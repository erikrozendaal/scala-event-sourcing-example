package com.zilverline.es2
package examples

import org.specs.Specification

import events._
import reports._

case class InstructionAdded(text: String)

case class InstructionIndex(instructions: List[String] = List.empty) extends Index {
  def applyEvent = {
    case Payload(event: InstructionAdded) => copy(event.text :: instructions)
  }
}

object InstructionsSpec extends Specification {

  val Source = newIdentifier

  val eventStore = new storage.EventStore
  val indexes = new Indexes

  indexes.add(InstructionIndex())

  eventStore.addListener(commit => indexes.process(commit))

  "instruction" should {
    "show up in index when added" in {
      eventStore.commit(Iterable(UncommittedEvent(Source, InstructionAdded("hello"))))

      indexes.get[InstructionIndex].instructions must contain("hello")
    }
  }
}
