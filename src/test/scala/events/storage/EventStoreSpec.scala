package com.zilverline.es2
package events.storage

import scala.collection._
import org.specs.Specification

import events._

object EventStoreSpec extends Specification {

  val StreamA = newIdentifier
  val StreamB = newIdentifier

  val subject = new EventStore

  "event store" should {
    "save single event" in {
      subject.commit(Iterable(Uncommitted(StreamA, ExampleEvent("example"))))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, ExampleEvent("example"))))
    }

    "save multiple events" in {
      subject.commit(Iterable(Uncommitted(StreamA, ExampleEvent("first"))))
      subject.commit(Iterable(Uncommitted(StreamA, ExampleEvent("second"))))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, ExampleEvent("first")),
        Committed(StreamA, ExampleEvent("second"))))
    }

    "save multiple events to different sources" in {
      subject.commit(Iterable(Uncommitted(StreamA, ExampleEvent("a"))))
      subject.commit(Iterable(Uncommitted(StreamB, ExampleEvent("b"))))

      subject.load(StreamA) must beEqualTo(Seq(Committed(StreamA, ExampleEvent("a"))))
      subject.load(StreamB) must beEqualTo(Seq(Committed(StreamB, ExampleEvent("b"))))
    }
  }

  "event store with listener" should {
    val received: mutable.Queue[(Identifier, Any)] = mutable.Queue()

    def listener: subject.EventStoreListener = {
      commit =>
        received += (commit.source -> commit.event)
    }

    subject.addListener(listener)

    "dispatch saved events to listener" in {
      subject.commit(Iterable(Uncommitted(StreamA, ExampleEvent("example"))))

      received must beEqualTo(Seq(StreamA -> ExampleEvent("example")))
    }

    "support multiple listeners" in {
      subject.addListener(listener)

      subject.commit(Iterable(Uncommitted(StreamA, ExampleEvent("example"))))

      received must beEqualTo(Seq(StreamA -> ExampleEvent("example"), StreamA -> ExampleEvent("example")))
    }
  }
}

