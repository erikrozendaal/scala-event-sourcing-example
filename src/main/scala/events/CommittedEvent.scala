package com.zilverline.es2

package events {
  case class CommittedEvent(source: EventSourceIdentifier, event: Event)
}
