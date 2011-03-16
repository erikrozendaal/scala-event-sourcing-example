package com.zilverline.es2
package reports

class ReportsSpec extends org.specs2.mutable.SpecificationWithJUnit {

  case class EventCountReport(count: Int) extends Report[DomainEvent] {
    def applyEvent = _ => copy(count + 1)
  }

  trait Context extends org.specs2.execute.Success {
    val Source = newIdentifier

    val subject = new Reports

    subject.register(EventCountReport(0))

    val eventCountReport = subject.queryable[EventCountReport]
  }

  "receive all events" in new Context {
    subject.applyEvent(Committed(Source, 1, ExampleEvent("hello")))

    subject.get[EventCountReport].count must beEqualTo(1)
    eventCountReport.query(_.count) must beEqualTo(1)
  }
}
