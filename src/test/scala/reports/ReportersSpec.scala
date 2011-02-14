package com.zilverline.es2

import org.specs.Specification

package reports {
  import events._

  object ExampleDocument {
    def apply(event: ExampleEvent) = new ExampleDocument(event.content)
  }
  case class ExampleDocument(content: String) extends Document {
    def applyEvent = {
      case event: ExampleEvent => copy(event.content)
    }
  }

  class ReportersSpec extends Specification {
    val subject = new Reporters

    val Source = newIdentifier

    def investigator: Investigator[ExampleEvent] = {
      case event => ExampleDocument(event)
    }

    "Reporters" should {
      subject.investigate(investigator)
      "generate specific reports based on initial lead" in {
        subject.update(Source, ExampleEvent("hello"))

        subject.retrieve[ExampleDocument](Source) must beEqualTo(ExampleDocument("hello"))
      }
    }

    "Specific reports" should {
      subject.store(Source, ExampleDocument("hello"))
      "be kept up-to-date of new events" in {
        subject.update(Source, ExampleEvent("goodbye"))

        subject.retrieve[ExampleDocument](Source) must beEqualTo(ExampleDocument("goodbye"))
      }
    }
  }
}
