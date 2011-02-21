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
      subject.commit(Iterable(Uncommitted(StreamA, 1, Event1)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1)))
    }

    "save multiple events" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, Event1)))
      subject.commit(Iterable(Uncommitted(StreamA, 2, Event2)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2)))
    }

    "save multiple events to different sources" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, Event1)))
      subject.commit(Iterable(Uncommitted(StreamB, 1, Event2)))

      subject.load(StreamA) must beEqualTo(Seq(Committed(StreamA, 1, Event1)))
      subject.load(StreamB) must beEqualTo(Seq(Committed(StreamB, 1, Event2)))
    }

    "ensure events are in order" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, Event1), Uncommitted(StreamA, 4, Event2))) must throwA[RuntimeException]
    }

    "throw optimistic locking exception on version mismatch for new event stream" in {
      subject.commit(Iterable(Uncommitted(StreamA, 2, Event1))) must throwA[OptimisticLockingException]
    }

    "throw optimistic locking exception on version mismatch for additional events" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, Event1)))

      subject.commit(Iterable(Uncommitted(StreamA, 1, Event2))) must throwA[OptimisticLockingException]
      subject.commit(Iterable(Uncommitted(StreamA, 3, Event2))) must throwA[OptimisticLockingException]
    }
  }

  "event store with listener" should {
    val received: mutable.Queue[(Identifier, Any)] = mutable.Queue()

    def listener: subject.EventStoreListener = {
      commit => received += commit.source -> commit.payload
    }

    subject.addListener(listener)

    "dispatch saved events to listener" in {
      subject.commit(Iterable(Uncommitted(StreamA, 1, Event1)))

      received must beEqualTo(Seq(StreamA -> Event1))
    }

    "support multiple listeners" in {
      subject.addListener(listener)

      subject.commit(Iterable(Uncommitted(StreamA, 1, Event1)))

      received must beEqualTo(Seq(StreamA -> Event1, StreamA -> Event1))
    }
  }
}

