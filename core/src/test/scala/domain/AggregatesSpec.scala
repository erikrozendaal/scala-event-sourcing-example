package com.zilverline.es2
package domain

import org.specs2.execute.Success

class AggregatesSpec extends org.specs2.mutable.SpecificationWithJUnit {

  case class ExampleAggregateRoot(id: Identifier, content: String) extends AggregateRoot {
    type Event = ExampleEvent

    def update(content: String) = updated(ExampleEvent(content))

    protected[this] def applyEvent = updated

    private def updated = when[ExampleEvent] {event => copy(content = event.content)}
  }

  object ExampleAggregateRoot extends AggregateFactory[ExampleAggregateRoot] {
    def create(id: Identifier, content: String) = created(id, ExampleEvent(content))

    protected[this] def applyEvent = created

    private def created = when[ExampleEvent] {event => ExampleAggregateRoot(event.eventSourceId, event.content)}
  }

  trait Context extends Success {
    val subject = new Aggregates(ExampleAggregateRoot)

    val TestId1 = newIdentifier

    val justCreated = ExampleAggregateRoot.create(TestId1, "hello").result
    val updated = behavior.pure(justCreated).flatMap(_.update("world")).result
    val different = ExampleAggregateRoot.create(TestId1, "different?").result
  }

  "aggregate store" should {
    "rebuild aggregates when replaying events" in new Context {
      subject applyEvent Committed(TestId1, 1, ExampleEvent("hello"))
      subject.get(TestId1) must beEqualTo(Some(Aggregate(TestId1, 1, justCreated)))

      subject applyEvent Committed(TestId1, 2, ExampleEvent("world"))
      subject.get(TestId1) must beEqualTo(Some(Aggregate(TestId1, 2, updated)))

      subject applyEvent Committed(TestId1, 1, ExampleEvent("old and out of order"))
      subject.get(TestId1) must beEqualTo(Some(Aggregate(TestId1, 2, updated)))
    }

    "ignore unknown event type" in new Context {
      subject applyEvent Committed(TestId1, 1, AnotherEvent("unknown"))
      subject.get(TestId1) must beNone
    }
  }
}
