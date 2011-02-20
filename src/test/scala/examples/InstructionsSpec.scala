package com.zilverline.es2
package examples

import org.specs.Specification

import reports._

case class InstructionAdded(text: String)

case class InstructionReport(instructions: List[String] = List.empty) extends Report[InstructionAdded] {
  def applyEvent = {
    case Payload(event: InstructionAdded) => copy(event.text :: instructions)
  }
}

object InstructionsSpec extends Specification {

  val Source = newIdentifier

  val eventStore = new eventstore.MemoryEventStore
  val reports = new Reports

  reports.register(InstructionReport())

  eventStore.addListener(commit => reports.applyEvent(commit))

  "instruction" should {
    "show up in index when added" in {
      eventStore.commit(Iterable(Uncommitted(Source, InstructionAdded("hello"))))

      reports.get[InstructionReport].instructions must contain("hello")
    }
  }
}
