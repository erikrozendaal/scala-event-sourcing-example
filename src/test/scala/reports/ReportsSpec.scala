package com.zilverline.es2
package reports

object ReportSpec extends org.specs.Specification {

  val Source = newIdentifier

  val subject = new Reports

  case class ExampleIndex(count: Int) extends Report[DomainEvent] {
    def applyEvent = { case _ => copy(count + 1) }
  }

  subject.register(ExampleIndex(0))

  "receive all events" in {
    subject.applyEvent(Committed(Source, ExampleEvent("hello")))

    subject.get[ExampleIndex].count must beEqualTo(1)
  }

}
