package com.zilverline.es2

package indexing {

  import eventing._
  import java.util.UUID
  import org.specs.Specification

  object IndexSpec extends Specification {

    val Source = UUID.randomUUID

    val subject = new Indexes

    case class ExampleIndex(count: Int) extends Index {
      def applyEvent = { _ => copy(count + 1) }
    }

    "index" should {
      subject.add(ExampleIndex(0))

      "receive all events" in {
        subject.process(ExampleEvent(Source, "hello"))

        subject.get[ExampleIndex].count must beEqualTo(1)
      }
    }

  }
}
