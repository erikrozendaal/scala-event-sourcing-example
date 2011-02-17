package com.zilverline.es2

sealed trait Recorded[+A <: DomainEvent] {
  def source: Identifier
  def event: A
}

case class Committed[+A <: DomainEvent](source: Identifier, event: A) extends Recorded[A]
case class Uncommitted[+A <: DomainEvent](source: Identifier, event: A) extends Recorded[A]

object Event {
  def unapply[A <: DomainEvent](recorded: Recorded[A]): Option[A] = Some(recorded.event)
}
