package com.zilverline.es2.eventing

trait Event {
  def source: EventSourceIdentifier
}
