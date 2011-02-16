package com.zilverline.es2
package reports

import events._

import org.specs.Specification

case class ExampleDocument(content: String) extends Document {
  def applyEvent = {
    case Payload(event: ExampleEvent) => copy(event.content)
  }
}

object DocumentsSpec extends Specification {
  val subject = new Documents

  val Source = newIdentifier

  def investigator: Investigator[ExampleEvent] = {
    case CommittedEvent(_, payload: ExampleEvent) => ExampleDocument(payload.content)
  }

  "Documents" should {
    subject.investigate(investigator)
    "generate specific documents based on initial event" in {
      subject.update(CommittedEvent(Source, ExampleEvent("hello")))

      subject.retrieve[ExampleDocument](Source) must beEqualTo(ExampleDocument("hello"))
    }
  }

  "Specific documents" should {
    subject.store(Source, ExampleDocument("hello"))
    "be kept up-to-date of new events" in {
      subject.update(CommittedEvent(Source, ExampleEvent("goodbye")))

      subject.retrieve[ExampleDocument](Source) must beEqualTo(ExampleDocument("goodbye"))
    }
  }
}
