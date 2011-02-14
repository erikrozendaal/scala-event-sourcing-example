package com.zilverline.es2

import org.specs.Specification

package examples {
  import events._
  import eventstore._
  import reports._

  case class InstructionAdded(text: String) extends Event

  case class InstructionIndex(instructions: List[String] = List.empty) extends Index {
    def applyEvent = {
      case event: InstructionAdded => copy(event.text :: instructions)
    }
  }

  object InstructionsSpec extends Specification {

    val Source = newIdentifier

    val eventStore = new EventStore
    val indexes = new Indexes

    indexes.add(InstructionIndex())

    eventStore.addListener(commit => indexes.process(commit.event))

    "instruction" should {
      "show up in index when added" in {
        eventStore.commit(Source, InstructionAdded("hello"))

        indexes.get[InstructionIndex].instructions must contain("hello")
      }
    }
  }

}