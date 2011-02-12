package com.zilverline.es2.eventing

case class ExampleEvent(source: EventSourceIdentifier, content: String) extends Event
