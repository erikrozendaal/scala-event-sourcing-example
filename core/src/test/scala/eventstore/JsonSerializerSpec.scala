package com.zilverline.es2
package eventstore

import EventGenerators._
import org.scalacheck.Prop._
import net.liftweb.json.{Serialization, FullTypeHints}
import java.nio.charset.{CharacterCodingException}

class JsonSerializerSpec extends org.specs2.mutable.SpecificationWithJUnit with org.specs2.ScalaCheck {

  "JsonSerializer" should {
    "(de)serialize events" in serializer().test
    "fail on JSON that cannot be encoded using UTF-8" in serializer().failOnBadUtf8
  }

  implicit val scalaCheckParameters = set(minTestsOk -> 10)
  implicit val formats = Serialization.formats(FullTypeHints(classOf[DomainEvent] :: Nil))

  case class serializer() {
    val subject = new JsonSerializer

    def test = forAll {event: ExampleEvent =>
      subject.encoder.canEncode(event.content) ==> {
        event == subject.deserialize(subject.serialize(event))
      }
    }

    def failOnBadUtf8 = {
      subject.serialize(ExampleEvent("\uDF3D")) must throwA[CharacterCodingException]
    }
  }
}
