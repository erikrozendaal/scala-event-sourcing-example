package com.zilverline.es2
package behavior

import eventstore._

class BehaviorSpec extends org.specs.Specification {

  val eventStore = new MemoryEventStore
  val emptyUnitOfWork = UnitOfWork()

  val Source = newIdentifier

  "track event source" in {
    val result = trackEventSource(Source, 3, "three").apply(emptyUnitOfWork).asInstanceOf[Accepted[_]]

    result.uow.eventSources(Source) must beEqualTo(EventSourceState(Source, 3, 3, "three", IndexedSeq.empty))
  }

  "track current event source revision" in {
    val result = trackEventSource(Source, 1, "original")
      .andThen(modifyEventSource(Source, ExampleEvent("example")){ _ => "example" })
      .apply(emptyUnitOfWork).asInstanceOf[Accepted[_]]

    result.uow.eventSources(Source) must beEqualTo(EventSourceState(Source, 1, 2, "example", IndexedSeq(ExampleEvent("example"))))
  }

}
