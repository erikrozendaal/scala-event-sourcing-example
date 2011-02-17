package com.zilverline

import java.util.UUID

package object es2 {
  type Identifier = UUID

  def newIdentifier: Identifier = UUID.randomUUID

  type DomainEvent = AnyRef
}
