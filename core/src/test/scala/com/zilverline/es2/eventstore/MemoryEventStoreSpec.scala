package com.zilverline.es2
package eventstore

class MemoryEventStoreSpec extends EventStoreSpec {
  override def makeEmptyEventStore(listeners: EventStore#EventStoreListener*) = new MemoryEventStore(listeners)
}
