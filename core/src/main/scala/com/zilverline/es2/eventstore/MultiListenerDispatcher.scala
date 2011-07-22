package com.zilverline.es2
package eventstore

/**
 * Dispatches each event to all listeners.
 */
case class MultiListenerDispatcher(listeners: CommittedEvent => Unit *) extends (Seq[CommittedEvent] => Unit) {
  override def apply(events: Seq[CommittedEvent]) {
    for (event <- events; listener <- listeners) {
      listener(event)
    }
  }
}
