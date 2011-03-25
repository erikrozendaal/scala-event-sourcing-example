package com.zilverline.es2
package eventstore

import net.liftweb.json._
import net.liftweb.json.Serialization.{read, write}

trait Serializer {
  def serialize(event: DomainEvent): String
  def deserialize(serialized: String): DomainEvent
}

class JsonSerializer(implicit formats: Formats)  extends Serializer {
  def serialize(event: DomainEvent): String = write(event)
  def deserialize(serialized: String): DomainEvent = read[DomainEvent](serialized)
}
