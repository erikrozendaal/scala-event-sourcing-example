package com.zilverline.es2

import org.scalacheck.Arbitrary._
import org.scalacheck.{Gen, Arbitrary}

trait TestEvent {
  val content: String
}
case class ExampleEvent(content: String) extends TestEvent
case class AnotherEvent(content: String) extends TestEvent

object EventGenerators {
  val unicodeLeadingSurrogate = '\uD800' to '\uDBFF'
  val unicodeTrailingSurrogate = '\uDC00' to '\uDFFF'
  val unicodeBasicMultilingualPlane = ('\u0000' to '\uFFFF') diff unicodeLeadingSurrogate diff unicodeTrailingSurrogate

  val unicodeCharacterBasicMultilingualPlane: Gen[String] =
    "basic multilingual plane" |: Gen.oneOf(unicodeBasicMultilingualPlane).map(_.toString)
  val unicodeCharacterSupplementaryPlane: Gen[String] =
    "supplementary plane" |:
      (for {
        c1 <- Gen.oneOf(unicodeLeadingSurrogate)
        c2 <- Gen.oneOf(unicodeTrailingSurrogate)
      } yield c1.toString + c2.toString)

  val unicodeCharacter: Gen[String] = Gen.frequency(
    9 -> unicodeCharacterBasicMultilingualPlane,
    1 -> unicodeCharacterSupplementaryPlane)

  val unicodeString = Gen.listOf(unicodeCharacter).map(_.mkString)

  implicit val arbitraryExampleEvent = Arbitrary(unicodeString.map(ExampleEvent))
  implicit val arbitraryAnotherEvent = Arbitrary(unicodeString.map(AnotherEvent))
  implicit val arbitraryTestEvent: Arbitrary[TestEvent] = Arbitrary(Gen.oneOf(arbitrary[ExampleEvent], arbitrary[AnotherEvent]))
}
