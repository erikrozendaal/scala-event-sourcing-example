package com.zilverline.es2.reporting

import com.zilverline.es2.eventing._
import java.util.UUID
import scala.collection._

trait SpecificReport {
  def applyEvent: Event => SpecificReport
}

class Reporters {
  def investigate[T <: Event](investigator: Investigator[T])(implicit m: Manifest[T]) {
    investigators.put(m.erasure, investigator)
  }

  def update(source: EventSourceIdentifier, event: Event) {
    reports.get(source) foreach {
      report => reports.put(source, report.applyEvent(event))
    }
    investigators.get(event.getClass)
      .flatMap(_.asInstanceOf[Investigator[Event]].lift.apply(event))
      .foreach(report => reports.put(source, report))
  }

  def store(source: EventSourceIdentifier, report: SpecificReport) {
    reports.put(source, report)
  }

  def retrieve[T <: SpecificReport](source: EventSourceIdentifier) = reports(source)

  private val investigators: mutable.Map[Class[_], Investigator[_]] = mutable.Map.empty
  private val reports: mutable.Map[EventSourceIdentifier, SpecificReport] = mutable.Map.empty
}
