package com.zilverline.es2

package reports {

  import events._
  import org.specs.Specification

  object IndexSpec extends Specification {

    val Source = newIdentifier

    val subject = new Indexes

    case class ExampleIndex(count: Int) extends Index {
      def applyEvent = { _ => copy(count + 1) }
    }

    "index" should {
      subject.add(ExampleIndex(0))

      "receive all events" in {
        subject.process(ExampleEvent("hello"))

        subject.get[ExampleIndex].count must beEqualTo(1)
      }
    }

  }
}
