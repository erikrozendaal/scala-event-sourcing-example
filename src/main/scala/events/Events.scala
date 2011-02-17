package com.zilverline.es2
package events

sealed trait RecordedEvent[+A <: DomainEvent] {
  def source: Identifier
  def payload: A
}

case class CommittedEvent[+A <: DomainEvent](source: Identifier, payload: A) extends RecordedEvent[A]
case class UncommittedEvent[+A <: DomainEvent](source: Identifier, payload: A) extends RecordedEvent[A]

object Payload {
  def unapply[A <: DomainEvent](event: RecordedEvent[A]): Option[A] = Some(event.payload)
}
