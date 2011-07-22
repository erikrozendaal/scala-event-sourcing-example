package com.zilverline.es2
package eventstore

import scala.collection._
import org.specs2.execute.Success

abstract class EventStoreSpec extends org.specs2.mutable.SpecificationWithJUnit {

  def makeEmptyEventStore(dispatcher: Seq[CommittedEvent] => Unit): EventStore

  trait Context extends Success {
    val StreamA = newIdentifier
    val StreamB = newIdentifier

    val Event1 = ExampleEvent("first")
    val Event2 = ExampleEvent("second")

    val subject = makeEmptyEventStore(events => ())
  }

  "event store" should {
    "save single event" in new Context {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1)))
    }

    "save multiple events" in new Context {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))
      subject.commit(Commit(StreamA, 1, Seq(Event2)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2)))
    }

    "save multiple events in single commit" in new Context {
      subject.commit(Commit(StreamA, 0, Seq(Event1, Event2)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2)))
    }

    "save multiple events in single commit followed by additional commits" in new Context {
      subject.commit(Commit(StreamA, 0, Seq(Event1, Event2)))
      subject.commit(Commit(StreamA, 2, Seq(Event1)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2),
        Committed(StreamA, 3, Event1)))
    }

    "save single event, multiple events, single event" in new Context {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))
      subject.commit(Commit(StreamA, 1, Seq(Event2, Event1)))
      subject.commit(Commit(StreamA, 3, Seq(Event2)))

      subject.load(StreamA) must beEqualTo(Seq(
        Committed(StreamA, 1, Event1),
        Committed(StreamA, 2, Event2),
        Committed(StreamA, 3, Event1),
        Committed(StreamA, 4, Event2)))
    }

    "save multiple events to different sources" in new Context {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))
      subject.commit(Commit(StreamB, 0, Seq(Event2)))

      subject.load(StreamA) must beEqualTo(Seq(Committed(StreamA, 1, Event1)))
      subject.load(StreamB) must beEqualTo(Seq(Committed(StreamB, 1, Event2)))
    }

    "throw optimistic locking exception on version mismatch for new event stream" in new Context {
      subject.commit(Commit(StreamA, 1, Seq(Event1))) must throwA[OptimisticLockingException]
    }

    "throw optimistic locking exception on version mismatch for additional events" in new Context {
      subject.commit(Commit(StreamA, 0, Seq(Event1)))

      subject.commit(Commit(StreamA, 0, Seq(Event2))) must throwA[OptimisticLockingException]
      subject.commit(Commit(StreamA, 2, Seq(Event2))) must throwA[OptimisticLockingException]
    }
  }

  "event store with listener" should {
    trait ContextWithListener extends Context {
      val received: mutable.Queue[(Identifier, Any)] = mutable.Queue()

      def listener(commit: Seq[CommittedEvent]) {
        received ++= commit.map(event => event.eventSourceId -> event.payload)
      }
    }

    "dispatch commits to listener" in new ContextWithListener {
      override val subject = makeEmptyEventStore(listener)

      subject.commit(Commit(StreamA, 0, Seq(Event1)))

      received must beEqualTo(Seq(StreamA -> Event1))
    }
  }
}
