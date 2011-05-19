package com.zilverline.es2

sealed trait Recorded[+A <: DomainEvent] {
  val eventSourceId: Identifier
  val payload: A
}

case class Committed[+A <: DomainEvent](eventSourceId: Identifier, sequence: Revision, payload: A) extends Recorded[A]
case class Uncommitted[+A <: DomainEvent](eventSourceId: Identifier, payload: A) extends Recorded[A]
