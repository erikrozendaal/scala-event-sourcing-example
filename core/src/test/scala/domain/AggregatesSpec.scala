package com.zilverline.es2
package domain

import org.specs2.execute.Success

class AggregatesSpec extends org.specs2.mutable.SpecificationWithJUnit {

  case class ExampleAggregateRoot(content: String) extends AggregateRoot {
    type Event = ExampleEvent

    def update(content: String) = updated(ExampleEvent(content))

    protected[this] def applyEvent = updated

    private def updated = when[ExampleEvent] {event => copy(content = event.content)}
  }

  object ExampleAggregateRoot extends AggregateFactory[ExampleAggregateRoot] {
    def create(id: Identifier, content: String): Behavior[ExampleAggregateRoot] = created(ExampleEvent(content))

    protected[this] def applyEvent = created

    private def created = when[ExampleEvent] {event => ExampleAggregateRoot(event.content)}
  }

  trait Context extends Success {
    implicit val subject = new Aggregates(ExampleAggregateRoot)

    val TestId1 = newIdentifier
    val Ref1 = Reference[ExampleAggregateRoot](TestId1)

    val justCreated = ExampleAggregateRoot("hello")
    val updated = justCreated.copy(content = "world")
    val different = ExampleAggregateRoot("different?")
  }

  "aggregate store" should {
    "rebuild aggregates when replaying events" in new Context {
      subject applyEvent Committed(TestId1, 1, ExampleEvent("hello"))
      subject.get(TestId1) must beSome(Aggregate(TestId1, 1, justCreated))

      subject applyEvent Committed(TestId1, 2, ExampleEvent("world"))
      subject.get(TestId1) must beSome(Aggregate(TestId1, 2, updated))

      subject applyEvent Committed(TestId1, 1, ExampleEvent("old and out of order"))
      subject.get(TestId1) must beSome(Aggregate(TestId1, 2, updated))
    }

    "ignore unknown event type" in new Context {
      subject applyEvent Committed(TestId1, 1, AnotherEvent("unknown"))
      subject.get(TestId1) must beNone
    }
  }

  "references" should {
    "fail when aggregate does not exist" in new Context {
      Reference[ExampleAggregateRoot](newIdentifier).run(_ => Behavior.pure()) must throwA[RuntimeException]
    }
    "use aggregates store to find initial version" in new Context {
      subject applyEvent Committed(TestId1, 1, ExampleEvent("hello"))

      Ref1.run(a => Behavior.pure(a)).result must_== ExampleAggregateRoot("hello")
    }
    "use global aggregates to find current version" in new Context {
      subject applyEvent Committed(TestId1, 1, ExampleEvent("hello"))

      val aggregate = Ref1.run(a => Behavior.pure(a)).result

      aggregate must_== ExampleAggregateRoot("hello")
    }
    "use tracked event sources to find the updated version" in new Context {
      val aggregate = Ref1.run {
        ExampleAggregateRoot.create(TestId1, "hello").flatMap(_.update("world")).then(Ref1.get)
      }.result

      aggregate must_== ExampleAggregateRoot("world")
    }
  }
}
