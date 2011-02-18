package com.zilverline.es2
package eventstore

import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

case class EventRecord(id: Long, aggregate: String, event: String) {
  def this() = this(0, "", "")
}

object SquerylEventStore extends Schema {
  val EventRecords = table[EventRecord]("event_records")
}

class SquerylEventStore {

}
