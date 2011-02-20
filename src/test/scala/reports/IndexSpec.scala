package com.zilverline.es2
package reports

object IndexSpec extends org.specs.Specification {

  val Source = newIdentifier

  val subject = new Indexes

  case class ExampleIndex(count: Int) extends Index {
    def applyEvent = { case _ => copy(count + 1) }
  }

  subject.add(ExampleIndex(0))

  "receive all events" in {
    subject.process(Committed(Source, ExampleEvent("hello")))

    subject.get[ExampleIndex].count must beEqualTo(1)
  }

}
