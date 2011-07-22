package com.zilverline.es2
package eventstore

import net.liftweb.json.Formats
import net.liftweb.json.Serialization.{read, write}
import java.io.{OutputStreamWriter, ByteArrayOutputStream, InputStreamReader, ByteArrayInputStream}
import java.nio.charset.Charset

trait Serializer {
  def serialize(event: DomainEvent): Array[Byte]
  def deserialize(serialized: Array[Byte]): DomainEvent
}

class JsonSerializer(implicit formats: Formats) extends Serializer {
  def charset = Charset.forName("UTF-8")
  def encoder = charset.newEncoder
  def decoder = charset.newDecoder

  def serialize(event: DomainEvent): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    write(event, new OutputStreamWriter(out, encoder))
    out.toByteArray
  }

  def deserialize(serialized: Array[Byte]): DomainEvent = {
    val in = new ByteArrayInputStream(serialized)
    read[DomainEvent](new InputStreamReader(in, decoder))
  }
}
