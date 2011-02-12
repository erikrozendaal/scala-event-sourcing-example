package com.zilverline.es2

import org.specs.Specification

package reporting {
  import eventing._

  object ExampleReport {
    def apply(event: ExampleEvent) = new ExampleReport(event.content)
  }
  case class ExampleReport(content: String) extends SpecificReport {
    def applyEvent = {
      case event: ExampleEvent => copy(event.content)
    }
  }

  class ReportersSpec extends Specification {
    val subject = new Reporters

    val Source = newIdentifier

    def investigator: Investigator[ExampleEvent] = {
      case event => ExampleReport(event)
    }

    "Reporters" should {
      subject.investigate(investigator)
      "generate specific reports based on initial lead" in {
        subject.update(ExampleEvent(Source, "hello"))

        subject.retrieve[ExampleReport](Source) must beEqualTo(ExampleReport("hello"))
      }
    }

    "Specific reports" should {
      subject.store(Source, ExampleReport("hello"))
      "be kept up-to-date of new events" in {
        subject.update(ExampleEvent(Source, "goodbye"))

        subject.retrieve[ExampleReport](Source) must beEqualTo(ExampleReport("goodbye"))
      }
    }
  }
}
