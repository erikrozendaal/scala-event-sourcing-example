package com.zilverline.es2
package domain

import behavior._
import org.specs2.execute.Success

class AggregatesSpec extends org.specs2.mutable.SpecificationWithJUnit {

  case class ExampleAggregateRoot(id: Identifier, content: String) extends AggregateRoot {
    type Event = ExampleEvent

    def update(content: String) = updated(ExampleEvent(content))

    protected[this] def applyEvent = updated

    private def updated = when[ExampleEvent] {event => copy(content = event.content)}
  }

  object ExampleAggregateRoot extends AggregateFactory[ExampleAggregateRoot] {
    def create(id: Identifier, content: String): Behavior[ExampleAggregateRoot] = created(id, ExampleEvent(content))

    protected[this] def applyEvent = created

    private def created = when[ExampleEvent] {event => ExampleAggregateRoot(event.eventSourceId, event.content)}
  }

  trait Context extends Success {
    val subject = new Aggregates(ExampleAggregateRoot)
    val repository = new AggregateRepository[ExampleAggregateRoot](subject)

    val TestId1 = newIdentifier

    val justCreated = ExampleAggregateRoot.create(TestId1, "hello").execute.result
    val updated = behavior.pure(justCreated).flatMap(_.update("world")).execute.result
    val different = ExampleAggregateRoot.create(TestId1, "different?").execute.result
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

  "aggregate repository" should {
    "fail when aggregate does not exist" in new Context {
      repository.get[ExampleAggregateRoot](newIdentifier).execute must throwA[IllegalArgumentException]
    }
    "use aggregates store to find initial version" in new Context {
      subject applyEvent Committed(TestId1, 1, ExampleEvent("hello"))

      repository.get[ExampleAggregateRoot](TestId1).execute.result must_== ExampleAggregateRoot(TestId1, "hello")
    }
    "use tracked event sources to find current version" in new Context {
      subject applyEvent Committed(TestId1, 1, ExampleEvent("hello"))

      val aggregate = repository.get[ExampleAggregateRoot](TestId1)
        .andThen(repository.get[ExampleAggregateRoot](TestId1))
        .execute.result

      aggregate must_== ExampleAggregateRoot(TestId1, "hello")
    }
    "use tracked event sources to find the updated version" in new Context {
      val aggregate = ExampleAggregateRoot.create(TestId1, "hello")
        .flatMap(_.update("world"))
        .andThen(repository.get[ExampleAggregateRoot](TestId1))
        .execute.result

      aggregate must_== ExampleAggregateRoot(TestId1, "world")
    }
  }
}
