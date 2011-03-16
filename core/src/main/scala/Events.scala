package com.zilverline.es2

sealed trait Recorded[+A <: DomainEvent] {
  def eventSourceId: Identifier
  def sequence: Revision
  def event: A
}

case class Committed[+A <: DomainEvent](eventSourceId: Identifier, sequence: Revision, event: A) extends Recorded[A]
case class Uncommitted[+A <: DomainEvent](eventSourceId: Identifier, sequence: Revision, event: A) extends Recorded[A]
