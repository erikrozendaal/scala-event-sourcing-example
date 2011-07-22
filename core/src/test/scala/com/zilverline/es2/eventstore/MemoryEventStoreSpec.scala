package com.zilverline.es2
package eventstore

class MemoryEventStoreSpec extends EventStoreSpec {
  def makeEmptyEventStore = new MemoryEventStore
}
