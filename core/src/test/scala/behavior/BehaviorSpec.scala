package com.zilverline.es2
package behavior

import eventstore._

class BehaviorSpec extends org.specs2.mutable.SpecificationWithJUnit {
  val EventSourceId = newIdentifier

  trait Context extends org.specs2.execute.Success {
    val eventStore = new MemoryEventStore
  }

  "track event source" in new Context {
    val result = trackEventSource(EventSourceId, 3, "three").execute

    result.tracked.eventSources(EventSourceId) must beEqualTo(
      TrackedEventSource(EventSourceId, 3, IndexedSeq.empty, Some("three")))
  }

  "track current event source revision" in new Context {
    val result = trackEventSource(EventSourceId, 1, "original")
      .andThen(modifyEventSource(EventSourceId, ExampleEvent("example")) {_.event.content})
      .execute

    result.tracked.eventSources(EventSourceId) must beEqualTo(
      TrackedEventSource(EventSourceId, 1, IndexedSeq(ExampleEvent("example")), Some("example")))
  }
}
