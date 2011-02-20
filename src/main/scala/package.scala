package com.zilverline

import java.util.UUID

package object es2 {
  type Identifier = UUID
  type Revision = Long

  def newIdentifier: Identifier = UUID.randomUUID

  type DomainEvent = AnyRef

  type RecordedEvent = Recorded[DomainEvent]
  type UncommittedEvent = Uncommitted[DomainEvent]
  type CommittedEvent = Committed[DomainEvent]
}
