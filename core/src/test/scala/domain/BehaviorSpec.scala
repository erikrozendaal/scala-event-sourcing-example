package com.zilverline.es2
package domain

import eventstore._
import org.specs2.execute.Result
import EventGenerators._
import org.scalacheck.Prop._

class BehaviorSpec extends org.specs2.mutable.SpecificationWithJUnit with org.specs2.ScalaCheck {

  "A behavior" should {
    "track event sources" in behavior().tracksEventSources
    "track single event source change" in behavior().tracksSingleChange
    "track multiple changes to a single event source" in behavior().tracksMultipleChangesToSingleEventSource
  }

  implicit val scalaCheckParameters = set(minTestsOk -> 10)

  case class behavior() {
    import Behavior._

    implicit val aggregates = new Aggregates()
    val EventSourceId = newIdentifier
    val eventStore = new MemoryEventStore

    def tracksEventSources: Result = forAll {(revision: Revision, value: String) =>
      (revision >= InitialRevision) ==> {
        val result = Behavior.run(Behavior.trackEventSource(EventSourceId, revision, value)).session.tracked(EventSourceId)

        result == TrackedEventSource(EventSourceId, revision, value, IndexedSeq.empty)
      }
    }

    def tracksSingleChange: Result = forAll {event: TestEvent =>
      val result = Behavior.run(Behavior.record(EventSourceId, event) {_.payload.content})
        .session.tracked(EventSourceId)

      result.changes == Seq(event) && result.value == event.content
    }

    def tracksMultipleChangesToSingleEventSource: Result = forAll {events: List[TestEvent] =>
      events.nonEmpty ==> {
        def record(events: List[TestEvent]): Behavior[Unit] = events match {
          case Nil => pure()
          case x::xs => Behavior.record(EventSourceId, x)(_.payload.content) then record(xs)
        }
        val result = Behavior.run(record(events)).session.tracked(EventSourceId)

        result.changes == events && result.value == events.last.content
      }
    }
  }
}
