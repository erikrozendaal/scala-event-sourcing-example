package com.zilverline.es2
package eventstore

object MemoryEventStoreSpec extends EventStoreSpec {
  val subject = new MemoryEventStore
}
