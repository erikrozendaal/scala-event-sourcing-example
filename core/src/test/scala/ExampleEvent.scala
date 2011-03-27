package com.zilverline.es2

import org.scalacheck.Arbitrary._
import org.scalacheck.Arbitrary

trait TestEvent
case class ExampleEvent(content: String) extends TestEvent
case class AnotherEvent(content: String) extends TestEvent

object EventGenerators {
  implicit val arbitraryExampleEvent = Arbitrary(for (content <- arbitrary[String]) yield ExampleEvent(content))
}
