package com.zilverline.es2

import scala.collection._
import org.specs.Specification

package eventstore {

  import eventing._

  object EventStoreSpec extends Specification {

    val StreamA = newIdentifier
    val StreamB = newIdentifier

    val subject = new EventStore

    "event store" should {
      "save single event" in {
        subject.save(ExampleEvent(StreamA, "example"))

        subject.load(StreamA) must beEqualTo(Seq(ExampleEvent(StreamA, "example")))
      }

      "save multiple events" in {
        subject.save(ExampleEvent(StreamA, "first"))
        subject.save(ExampleEvent(StreamA, "second"))

        subject.load(StreamA) must beEqualTo(Seq(ExampleEvent(StreamA, "first"), ExampleEvent(StreamA, "second")))
      }

      "save multiple events to different sources" in {
        subject.save(ExampleEvent(StreamA, "a"))
        subject.save(ExampleEvent(StreamB, "b"))

        subject.load(StreamA) must beEqualTo(Seq(ExampleEvent(StreamA, "a")))
        subject.load(StreamB) must beEqualTo(Seq(ExampleEvent(StreamB, "b")))
      }
    }

    "event store with listener" should {
      val received: mutable.Queue[(EventSourceIdentifier, AnyRef)] = mutable.Queue()

      def listener: subject.EventListener = {
        (source, event) =>
          received += (source -> event)
      }

      subject.addListener(listener)

      "dispatch saved events to listener" in {
        subject.save(ExampleEvent(StreamA, "example"))

        received must beEqualTo(Seq(StreamA -> ExampleEvent(StreamA, "example")))
      }

      "support multiple listeners" in {
        subject.addListener(listener)

        subject.save(ExampleEvent(StreamA, "example"))

        received must beEqualTo(Seq(StreamA -> ExampleEvent(StreamA, "example"), StreamA -> ExampleEvent(StreamA, "example")))
      }
    }
  }

}
