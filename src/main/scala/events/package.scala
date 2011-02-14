package com.zilverline.es2

import java.util.UUID

package object events {
  type EventSourceIdentifier = UUID

  def newIdentifier: EventSourceIdentifier = UUID.randomUUID
}
