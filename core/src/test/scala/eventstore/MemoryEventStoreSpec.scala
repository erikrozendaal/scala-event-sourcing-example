package com.zilverline.es2
package eventstore

class MemoryEventStoreSpec extends EventStoreSpec {
  val subject = new MemoryEventStore
}
