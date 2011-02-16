package com.zilverline.es2
package events

case class CommittedEvent(source: Identifier, payload: AnyRef)

object Payload {
  def unapply(event: CommittedEvent): Option[AnyRef] = Some(event.payload)
}
