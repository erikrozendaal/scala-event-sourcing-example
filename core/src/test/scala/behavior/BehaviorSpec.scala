package com.zilverline.es2
package behavior

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
    val EventSourceId = newIdentifier
    val eventStore = new MemoryEventStore

    def tracksEventSources: Result = forAll {(revision: Revision, value: String) =>
      (revision >= InitialRevision) ==> {
        val result = Behavior.run(Behavior.trackEventSource(EventSourceId, revision, value)).tracked.eventSources(EventSourceId)

        result == TrackedEventSource(EventSourceId, revision, value, IndexedSeq.empty)
      }
    }

    def tracksSingleChange: Result = forAll {event: TestEvent =>
      val result = Behavior.run(Behavior.modifyEventSource(EventSourceId, event) {_.payload.content})
        .tracked.eventSources(EventSourceId)

      result.changes == Seq(event) &&
        result.currentRevision == InitialRevision + 1 &&
        result.currentValue == event.content
    }

    def tracksMultipleChangesToSingleEventSource: Result = forAll {events: List[TestEvent] =>
      events.nonEmpty ==> {
        val result = Behavior.run(events foreach { event =>
          Behavior.modifyEventSource(EventSourceId, event)(_.payload.content)
        }).tracked.eventSources(EventSourceId)

        result.changes == events &&
          result.currentRevision == InitialRevision + events.size &&
          result.currentValue == events.last.content
      }
    }
  }
}
