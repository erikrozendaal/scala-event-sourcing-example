package com.zilverline.es2

trait TestEvent
case class ExampleEvent(content: String) extends TestEvent
case class AnotherEvent(content: String) extends TestEvent
