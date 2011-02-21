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

class InstructionsSpec extends Specification {

  val Source = newIdentifier

  val eventStore = new eventstore.MemoryEventStore
  val reports = new Reports

  reports.register(InstructionReport())

  eventStore.addListener(commit => reports.applyEvent(commit))

  "instruction" should {
    "show up in index when added" in {
      eventStore.commit(Iterable(Uncommitted(Source, 1, InstructionAdded("hello"))))

      reports.queryable[InstructionReport].query(_.instructions) must contain("hello")
    }
  }
}
