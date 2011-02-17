package com.zilverline.es2
package reports

import scala.collection._

trait Document {
  def applyEvent: Committed[AnyRef] => Document
}

class Documents {
  def investigate[T <: DomainEvent](investigator: Investigator[T])(implicit m: Manifest[T]) {
    investigators.put(m.erasure, investigator)
  }

  def update(event: Committed[AnyRef]) {
    reports.get(event.source) foreach {
      report => reports.put(event.source, report.applyEvent(event))
    }
    investigators.get(event.event.getClass)
      .flatMap(_.asInstanceOf[Investigator[AnyRef]].lift.apply(event))
      .foreach(report => reports.put(event.source, report))
  }

  def store(source: Identifier, document: Document) {
    reports.put(source, document)
  }

  def retrieve[T <: Document](source: Identifier) = reports(source)

  private val investigators: mutable.Map[Class[_], Investigator[_]] = mutable.Map.empty
  private val reports: mutable.Map[Identifier, Document] = mutable.Map.empty
}
