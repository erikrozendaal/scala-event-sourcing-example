package com.zilverline.es2
package eventstore

import scala.collection._
import org.specs.Specification

abstract class EventStoreSpec extends Specification {

  val StreamA = newIdentifier
  val StreamB = newIdentifier

  val Event1 = ExampleEvent("first")
  val Event2 = ExampleEvent("second")

  val subject: EventStore

  "event store" should {
    "save single event" in {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1)))
    }

    "save multiple events" in {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))
      subject.commit(Commit(StreamA, 1, Seq(Event2)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2)))
    }

    "save multiple events in single commit" in {
      subject.commit(Commit(StreamA, 0, Seq(Event1, Event2)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2)))
    }

    "save multiple events in single commit followed by additional commits" in {
      subject.commit(Commit(StreamA, 0, Seq(Event1, Event2)))
      subject.commit(Commit(StreamA, 2, Seq(Event1)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2),
        Committed(StreamA, 3, Event1)))
    }

    "save single event, multiple events, single event" in {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))
      subject.commit(Commit(StreamA, 1, Seq(Event2, Event1)))
      subject.commit(Commit(StreamA, 3, Seq(Event2)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2),
        Committed(StreamA, 3, Event1),
        Committed(StreamA, 4, Event2)))
    }

    "save multiple events to different sources" in {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))
      subject.commit(Commit(StreamB, 0, Seq(Event2)))

      subject.load(StreamA) must beEqualTo(Seq(Committed(StreamA, 1, Event1)))
      subject.load(StreamB) must beEqualTo(Seq(Committed(StreamB, 1, Event2)))
    }

    "throw optimistic locking exception on version mismatch for new event stream" in {
      subject.commit(Commit(StreamA, 1, Seq(Event1))) must throwA[OptimisticLockingException]
    }

    "throw optimistic locking exception on version mismatch for additional events" in {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))

      subject.commit(Commit(StreamA, 0, Seq(Event2))) must throwA[OptimisticLockingException]
      subject.commit(Commit(StreamA, 2, Seq(Event2))) must throwA[OptimisticLockingException]
    }
  }

  "event store with listener" should {
    val received: mutable.Queue[(Identifier, Any)] = mutable.Queue()

    def listener: subject.EventStoreListener = {
      commit => received += commit.eventSourceId -> commit.event
    }

    subject.addListener(listener)

    "dispatch saved events to listener" in {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))

      received must beEqualTo(Seq(StreamA -> Event1))
    }

    "support multiple listeners" in {
      subject.addListener(listener)

      subject.commit(Commit(StreamA, 0, Seq(Event1)))

      received must beEqualTo(Seq(StreamA -> Event1, StreamA -> Event1))
    }
  }
}
