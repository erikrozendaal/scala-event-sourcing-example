package com.zilverline.es2
package reports

import util._

import scala.collection.mutable.{ArrayBuffer, Map => MMap}

trait Report extends EventProcessor[DomainEvent, Report]

class Reports {
  def add(report: Report) {
    reportByType.put(report.getClass, new AtomicVar(report))
  }

  def get[T <: Report](implicit m: Manifest[T], nn: NotNothing[T]): T = reportByType(m.erasure).get.asInstanceOf[T]

  def process(event: CommittedEvent) {
    reportByType.values.foreach { report => report.modifyWithRetry(_.applyEvent(event)) }
  }

  private val reportByType: MMap[Class[_], AtomicVar[Report]] = MMap.empty
}
