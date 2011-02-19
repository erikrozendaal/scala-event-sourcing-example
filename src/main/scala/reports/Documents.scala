package com.zilverline.es2
package reports

import scala.collection._

trait Document {
  def applyEvent: CommittedEvent => Document
}

class Documents {
  def investigate[T <: DomainEvent](factory: DocumentFactory[T])(implicit m: Manifest[T]) {
    factories.put(m.erasure, factory)
  }

  def update(committed: CommittedEvent) {
    reports.get(committed.source) foreach {
      report => reports.put(committed.source, report.applyEvent(committed))
    }
    factories.get(committed.payload.getClass)
      .flatMap(_.asInstanceOf[DocumentFactory[AnyRef]].lift.apply(committed))
      .foreach(report => store(committed.source, report))
  }

  def store(source: Identifier, document: Document) {
    reports.put(source, document)
  }

  def get[T <: Document : NotNothing](source: Identifier): T = reports(source).asInstanceOf[T]

  private val factories: mutable.Map[Class[_], DocumentFactory[_]] = mutable.Map.empty
  private val reports: mutable.Map[Identifier, Document] = mutable.Map.empty
}
