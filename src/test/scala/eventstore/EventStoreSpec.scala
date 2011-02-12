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
        subject.save(StreamA, ExampleEvent("example"))

        subject.load(StreamA) must beEqualTo(Seq(ExampleEvent("example")))
      }

      "save multiple events" in {
        subject.save(StreamA, ExampleEvent("first"))
        subject.save(StreamA, ExampleEvent("second"))

        subject.load(StreamA) must beEqualTo(Seq(ExampleEvent("first"), ExampleEvent("second")))
      }

      "save multiple events to different sources" in {
        subject.save(StreamA, ExampleEvent("a"))
        subject.save(StreamB, ExampleEvent("b"))

        subject.load(StreamA) must beEqualTo(Seq(ExampleEvent("a")))
        subject.load(StreamB) must beEqualTo(Seq(ExampleEvent("b")))
      }
    }

    "event store with listener" should {
      val received: mutable.Queue[(EventSourceIdentifier, AnyRef)] = mutable.Queue()

      def listener: subject.EventStoreListener = {
        commit =>
          received += (commit.source -> commit.event)
      }

      subject.addListener(listener)

      "dispatch saved events to listener" in {
        subject.save(StreamA, ExampleEvent("example"))

        received must beEqualTo(Seq(StreamA -> ExampleEvent("example")))
      }

      "support multiple listeners" in {
        subject.addListener(listener)

        subject.save(StreamA, ExampleEvent("example"))

        received must beEqualTo(Seq(StreamA -> ExampleEvent("example"), StreamA -> ExampleEvent("example")))
      }
    }
  }

}
