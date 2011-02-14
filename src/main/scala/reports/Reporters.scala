package com.zilverline.es2.reports

import com.zilverline.es2.events._
import java.util.UUID
import scala.collection._

trait Document {
  def applyEvent: Event => Document
}

class Reporters {
  def investigate[T <: Event](investigator: Investigator[T])(implicit m: Manifest[T]) {
    investigators.put(m.erasure, investigator)
  }

  def update(source: Identifier, event: Event) {
    reports.get(source) foreach {
      report => reports.put(source, report.applyEvent(event))
    }
    investigators.get(event.getClass)
      .flatMap(_.asInstanceOf[Investigator[Event]].lift.apply(event))
      .foreach(report => reports.put(source, report))
  }

  def store(source: Identifier, document: Document) {
    reports.put(source, document)
  }

  def retrieve[T <: Document](source: Identifier) = reports(source)

  private val investigators: mutable.Map[Class[_], Investigator[_]] = mutable.Map.empty
  private val reports: mutable.Map[Identifier, Document] = mutable.Map.empty
}
