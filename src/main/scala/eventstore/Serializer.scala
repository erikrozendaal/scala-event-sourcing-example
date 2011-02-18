package com.zilverline.es2
package eventstore

import net.liftweb.json._
import net.liftweb.json.Serialization.{read, write}

trait Serializer {
  def serialize(event: DomainEvent): String
  def deserialize(serialized: String): DomainEvent
}

class ReflectionTypeHints extends TypeHints {
  val hints = Nil
  override def containsHint_?(clazz: Class[_]) = true
  def classFor(hint: String): Option[Class[_]] = Some(Class.forName(hint))
  def hintFor(clazz: Class[_]): String = clazz.getName
}

class JsonSerializer(implicit formats: Formats)  extends Serializer {
  def serialize(event: DomainEvent): String = write(event)
  def deserialize(serialized: String): DomainEvent = read[DomainEvent](serialized)
}
