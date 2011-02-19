package com.zilverline.es2

sealed trait Recorded[+A <: DomainEvent] {
  def source: Identifier
  def event: A
}

case class Committed[+A <: DomainEvent](source: Identifier, event: A) extends Recorded[A]
case class Uncommitted[+A <: DomainEvent](source: Identifier, event: A) extends Recorded[A]

object & {
  def unapply[A](a: A): Option[(A, A)] = Some(a, a)
}

object Source {
  def unapply(recorded: Recorded[_]): Option[Identifier] = Some(recorded.source)
}

object Payload {
  def unapply[A <: DomainEvent](recorded: Recorded[A]): Option[A] = Some(recorded.event)
}
