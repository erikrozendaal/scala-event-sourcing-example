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
      subject.commit(Iterable(UncommittedEvent(StreamA, ExampleEvent("example"))))

      subject.load(StreamA) must beEqualTo(Seq(
        CommittedEvent(StreamA, ExampleEvent("example"))))
    }

    "save multiple events" in {
      subject.commit(Iterable(UncommittedEvent(StreamA, ExampleEvent("first"))))
      subject.commit(Iterable(UncommittedEvent(StreamA, ExampleEvent("second"))))

      subject.load(StreamA) must beEqualTo(Seq(
        CommittedEvent(StreamA, ExampleEvent("first")),
        CommittedEvent(StreamA, ExampleEvent("second"))))
    }

    "save multiple events to different sources" in {
      subject.commit(Iterable(UncommittedEvent(StreamA, ExampleEvent("a"))))
      subject.commit(Iterable(UncommittedEvent(StreamB, ExampleEvent("b"))))

      subject.load(StreamA) must beEqualTo(Seq(CommittedEvent(StreamA, ExampleEvent("a"))))
      subject.load(StreamB) must beEqualTo(Seq(CommittedEvent(StreamB, ExampleEvent("b"))))
    }
  }

  "event store with listener" should {
    val received: mutable.Queue[(Identifier, Any)] = mutable.Queue()

    def listener: subject.EventStoreListener = {
      commit =>
        received += (commit.source -> commit.payload)
    }

    subject.addListener(listener)

    "dispatch saved events to listener" in {
      subject.commit(Iterable(UncommittedEvent(StreamA, ExampleEvent("example"))))

      received must beEqualTo(Seq(StreamA -> ExampleEvent("example")))
    }

    "support multiple listeners" in {
      subject.addListener(listener)

      subject.commit(Iterable(UncommittedEvent(StreamA, ExampleEvent("example"))))

      received must beEqualTo(Seq(StreamA -> ExampleEvent("example"), StreamA -> ExampleEvent("example")))
    }
  }
}

