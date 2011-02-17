package com.zilverline.es2

object EventProcessorSpec extends org.specs.Specification {

  val Aggregate = newIdentifier
  val ExampleCommitted = Committed(Aggregate, ExampleEvent("example"))
  val AnotherCommitted = Committed(Aggregate, AnotherEvent("another"))

  var processed: Option[String] = None

  val subject = EventProcessor[ExampleEvent, Unit] {
    case Source(id) & Event(event: ExampleEvent) =>
      processed = Some(event.content)
  }

  "event processor" should {
    "process applicable domain event" in {
      subject.applyEvent(ExampleCommitted)

      processed must beEqualTo(Some("example"))
    }

    // "know if event is applicable" in {
    //   subject.isApplicable(ExampleCommitted) must beTrue
    //   subject.isApplicable(AnotherCommitted) must beFalse
    // }

    // "return type of applicable domain event" in {
    //   val expected: AnyRef = classOf[ExampleEvent]
    //   val actual: AnyRef = subject.domainEventType

    //   actual must beEqualTo(expected)
    // }
  }

}
