package com.zilverline.es2

package events {
  case class Commit(source: EventSourceIdentifier, event: Event)
}
