package com.zilverline.es2
package eventstore

@org.junit.runner.RunWith(classOf[org.specs2.runner.JUnitRunner])
class MemoryEventStoreSpec extends EventStoreSpec {
  override def makeEmptyEventStore(dispatcher: Seq[CommittedEvent] => Unit) = new MemoryEventStore(dispatcher)
}
