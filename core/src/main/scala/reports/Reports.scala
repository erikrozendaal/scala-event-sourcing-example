package com.zilverline.es2
package reports

import util._

import scala.collection.mutable.{ArrayBuffer, Map => MMap}

trait Report[-A <: DomainEvent] extends EventProcessor[A, Report[A]]

class Reports extends EventProcessor[DomainEvent, Unit] {
  def register[A <: DomainEvent](report: Report[A])(implicit m: Manifest[A]) {
    require(!reportByType.contains(report.getClass), "report of type " + report.getClass.getName + " already registered")

    val reportVar = new AtomicVar(report)
    reportByType += report.getClass -> reportVar.asInstanceOf[AtomicVar[Report[_]]]
    reportsByEventType = reportsByEventType + (m.erasure -> {
        event: CommittedEvent =>
          reportVar.modifyWithRetry(_.applyEvent(event.asInstanceOf[Committed[A]]))
    })
  }

  def get[T <: Report[_]](implicit m: Manifest[T], nn: NotNothing[T]): T = reportByType(m.erasure).get.asInstanceOf[T]

  def applyEvent = event => {
    reportsByEventType.getMostSpecific(event.payload.getClass) foreach {
      updater => updater(event)
    }
  }

  private val reportByType: MMap[Class[_], AtomicVar[Report[_]]] = MMap.empty
  private var reportsByEventType: TypeMap[CommittedEvent => Unit] = TypeMap.empty
}
