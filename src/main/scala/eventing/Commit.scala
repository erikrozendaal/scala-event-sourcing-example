package com.zilverline.es2

package eventing {
  case class Commit(source: EventSourceIdentifier, event: Event)
}
