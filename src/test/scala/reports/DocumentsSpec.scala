package com.zilverline.es2
package reports

import org.specs.Specification

case class ExampleDocument(content: String) extends Document {
  def applyEvent = {
    case Payload(event: ExampleEvent) => copy(event.content)
  }
}

object DocumentsSpec extends Specification {
  val subject = new Documents

  val Source = newIdentifier

  def investigator: DocumentFactory[ExampleEvent] = {
    case Payload(event: ExampleEvent) => ExampleDocument(event.content)
  }

  "Documents" should {
    subject.investigate(investigator)
    "generate specific documents based on initial event" in {
      subject.update(Committed(Source, ExampleEvent("hello")))

      subject.get[ExampleDocument](Source) must beEqualTo(ExampleDocument("hello"))
    }
  }

  "Specific documents" should {
    subject.store(Source, ExampleDocument("hello"))
    "be kept up-to-date of new events" in {
      subject.update(Committed(Source, ExampleEvent("goodbye")))

      subject.get[ExampleDocument](Source) must beEqualTo(ExampleDocument("goodbye"))
    }
  }
}
