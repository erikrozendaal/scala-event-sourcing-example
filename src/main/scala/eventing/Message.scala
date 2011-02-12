package com.zilverline.es2

package eventing {
  case class Message(source: EventSourceIdentifier, payload: Event)
}
