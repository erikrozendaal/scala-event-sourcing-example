package com.zilverline.es2
package behavior

import eventstore._

class BehaviorSpec extends org.specs2.mutable.SpecificationWithJUnit {

  trait Context extends org.specs2.execute.Success {
    val eventStore = new MemoryEventStore
    val emptyUnitOfWork = TrackedEventSources()

    val Source = newIdentifier
  }

  "track event source" in new Context {
    val result = trackEventSource(Source, 3, "three").apply(emptyUnitOfWork)

    result.tracked.eventSources(Source) must beEqualTo(TrackedEventSource(Source, 3, IndexedSeq.empty, Some("three")))
  }

  "track current event source revision" in new Context {
    val result = trackEventSource(Source, 1, "original")
      .andThen(modifyEventSource(Source, ExampleEvent("example")){ _ => "example" })
      .apply(emptyUnitOfWork)

    result.tracked.eventSources(Source) must beEqualTo(TrackedEventSource(Source, 1, IndexedSeq(ExampleEvent("example")), Some("example")))
  }
}
