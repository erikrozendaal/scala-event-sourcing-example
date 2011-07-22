package com.zilverline.es2
package eventstore

import EventGenerators._
import org.scalacheck.Prop._
import net.liftweb.json.{Serialization, FullTypeHints}
import java.nio.charset.{CharacterCodingException}

class JsonSerializerSpec extends org.specs2.mutable.SpecificationWithJUnit with org.specs2.ScalaCheck {

  "JsonSerializer" should {
    "(de)serialize arbitrary events" in context().serialization
    "fail on JSON that contains bad unicode characters" in context().failOnBadUnicode
  }

  case class context() {
    implicit val formats = Serialization.formats(FullTypeHints(classOf[DomainEvent] :: Nil))
    val subject = new JsonSerializer

    def serialization = forAll {event: TestEvent =>
      event == subject.deserialize(subject.serialize(event))
    }

    def failOnBadUnicode = {
      subject.serialize(ExampleEvent("\uDF3D")) must throwA[CharacterCodingException]
    }
  }
}
