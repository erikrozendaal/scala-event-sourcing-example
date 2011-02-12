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

  def update(message: Message) {
    reports.get(message.source) foreach {
      report => reports.put(message.source, report.applyEvent(message.payload))
    }
    investigators.get(message.payload.getClass)
      .flatMap(_.asInstanceOf[Investigator[Event]].lift.apply(message.payload))
      .foreach(report => reports.put(message.source, report))
  }

  def store(source: EventSourceIdentifier, report: SpecificReport) {
    reports.put(source, report)
  }

  def retrieve[T <: SpecificReport](source: EventSourceIdentifier) = reports(source)

  private val investigators: mutable.Map[Class[_], Investigator[_]] = mutable.Map.empty
  private val reports: mutable.Map[EventSourceIdentifier, SpecificReport] = mutable.Map.empty
}
