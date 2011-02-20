package com.zilverline.es2
package eventstore

import scala.collection._
import org.specs.Specification

abstract class EventStoreSpec extends Specification {

  val StreamA = newIdentifier
  val StreamB = newIdentifier

  val subject: EventStore

  "event store" should {
    "save single event" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, ExampleEvent("example"))))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, ExampleEvent("example"))))
    }

    "save multiple events" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, ExampleEvent("first"))))
      subject.commit(Iterable(Uncommitted(StreamA, 2, ExampleEvent("second"))))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, ExampleEvent("first")),
        Committed(StreamA, 2, ExampleEvent("second"))))
    }

    "save multiple events to different sources" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, ExampleEvent("a"))))
      subject.commit(Iterable(Uncommitted(StreamB, 1, ExampleEvent("b"))))

      subject.load(StreamA) must beEqualTo(Seq(Committed(StreamA, 1, ExampleEvent("a"))))
      subject.load(StreamB) must beEqualTo(Seq(Committed(StreamB, 1, ExampleEvent("b"))))
    }

    "throw optimistic locking exception on version mismatch" in {

    }
  }

  "event store with listener" should {
    val received: mutable.Queue[(Identifier, Any)] = mutable.Queue()

    def listener: subject.EventStoreListener = {
      commit => received += commit.source -> commit.payload
    }

    subject.addListener(listener)

    "dispatch saved events to listener" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, ExampleEvent("example"))))

      received must beEqualTo(Seq(StreamA -> ExampleEvent("example")))
    }

    "support multiple listeners" in {
      subject.addListener(listener)

      subject.commit(Iterable(Uncommitted(StreamA, 1, ExampleEvent("example"))))

      received must beEqualTo(Seq(StreamA -> ExampleEvent("example"), StreamA -> ExampleEvent("example")))
    }
  }
}

