package com.zilverline.es2.eventstore

import scala.collection._
import java.util.UUID
import org.specs.Specification

object EventStoreSpec extends Specification {

  case class ExampleEvent(content: String)

  val StreamA = UUID.randomUUID
  val StreamB = UUID.randomUUID

  val subject = new EventStore

  "event store" should {
    "save single event" in {
      subject.save(StreamA, ExampleEvent("example"))

      subject.load(StreamA) must beEqualTo(Seq(ExampleEvent("example")))
    }

    "save multiple events" in {
      subject.save(StreamA, ExampleEvent("first"))
      subject.save(StreamA, ExampleEvent("second"))

      subject.load(StreamA) must beEqualTo(Seq(ExampleEvent("first"), ExampleEvent("second")))
    }

    "save multiple events to different sources" in {
      subject.save(StreamA, ExampleEvent("a"))
      subject.save(StreamB, ExampleEvent("b"))

      subject.load(StreamA) must beEqualTo(Seq(ExampleEvent("a")))
      subject.load(StreamB) must beEqualTo(Seq(ExampleEvent("b")))
    }
  }

  "event store with listener" should {
    val received: mutable.Queue[(UUID, AnyRef)] = mutable.Queue()

    subject.listen {
      (source, event) =>
        received += (source -> event)
    }

    "dispatch saved events to listener" in {
      subject.save(StreamA, ExampleEvent("example"))

      received must beEqualTo(Seq(StreamA -> ExampleEvent("example")))
    }
  }
}
