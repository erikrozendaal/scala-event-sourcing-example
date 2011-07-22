package com.zilverline.es2
package eventstore

class MemoryEventStoreSpec extends EventStoreSpec {
  override def makeEmptyEventStore(dispatcher: Seq[CommittedEvent] => Unit) = new MemoryEventStore(dispatcher)
}
