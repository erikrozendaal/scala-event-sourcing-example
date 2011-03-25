package com.zilverline

package object es2 {
  type Revision = Long
  val InitialRevision: Revision = 0L

  def newIdentifier: Identifier = Identifier()

  type DomainEvent = AnyRef

  type RecordedEvent = Recorded[DomainEvent]
  type UncommittedEvent = Uncommitted[DomainEvent]
  type CommittedEvent = Committed[DomainEvent]
}
