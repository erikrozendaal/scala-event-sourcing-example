package com.zilverline.es2
package domain

import eventstore._

class AggregatesSpec extends org.specs.Specification {

  case class TestAR1(id: Identifier, content: String) extends AggregateRoot {
    type Event = ExampleEvent

    def update(content: String) = updated(ExampleEvent(content))

    protected[this] def applyEvent = updated

    private def updated = when[ExampleEvent] {event => copy(content = event.content)}
  }

  case class InitialTestAR1(id: Identifier) extends AggregateRoot {
    type Event = ExampleEvent

    def update(content: String) = updated(ExampleEvent(content))

    protected[this] def applyEvent = updated

    private def updated = when[ExampleEvent] {event => TestAR1(id, event.content)}
  }

  val subject = new Aggregates(InitialTestAR1.apply _)

  val TestId1 = newIdentifier
  val justCreated = InitialTestAR1(TestId1).update("hello").result
  val updated = transaction.pure(justCreated).flatMap(_.update("world")).result
  val different = InitialTestAR1(TestId1).update("different?").result

  "new aggregate store" should {
    subject.putIfNewer(updated, 2)

    "store aggregates with revision number" in {
      subject.get(TestId1) must beEqualTo(Some(2, updated))
    }

    "not update stored aggregate when revision is equal or lower" in {
      subject.putIfNewer(different, 1)

      subject.get(TestId1) must beEqualTo(Some(2, updated))
    }

    "only update stored aggregate when revision is higher" in {
      subject.putIfNewer(different, 3)

      subject.get(TestId1) must beEqualTo(Some(3, different))
    }
  }

  "aggregate store" should {
    "rebuild aggregates when replaying events" in {
      subject applyEvent Committed(TestId1, 1, ExampleEvent("hello"))
      subject.get(TestId1) must beEqualTo(Some(1, justCreated))

      subject applyEvent Committed(TestId1, 2, ExampleEvent("world"))
      subject.get(TestId1) must beEqualTo(Some(2, updated))

      subject applyEvent Committed(TestId1, 1, ExampleEvent("old and out of order"))
      subject.get(TestId1) must beEqualTo(Some(2, updated))
    }

    "ignore unknown event type" in {
      subject applyEvent Committed(TestId1, 1, AnotherEvent("unknown"))
      subject.get(TestId1) must beNone
    }
  }
}
