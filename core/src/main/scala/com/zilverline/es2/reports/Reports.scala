package com.zilverline.es2
package reports

import util._

import scala.collection.mutable.{Map => MMap}
import scala.concurrent.stm._

trait Report[-A <: DomainEvent] extends EventProcessor[A, Report[A]]

trait ReportRef[+A <: Report[_]] {
  def query[B](f: A => B): B
}

class Reports extends EventProcessor[DomainEvent, Unit] {

  def register[A <: DomainEvent](report: Report[A])(implicit m: Manifest[A]) {
    require(!reportByType.contains(report.getClass), "report of type " + report.getClass.getName + " already registered")

    val reportVar = Ref(report).single
    reportByType += report.getClass -> reportVar.asInstanceOf[Ref.View[Report[_]]]
    reportsByEventType = reportsByEventType + (m.erasure -> {
        event: CommittedEvent =>
          reportVar.transform(_.applyEvent(event.asInstanceOf[Committed[A]]))
    })
  }

  def queryable[A <: Report[_] : Manifest : NotNothing]: ReportRef[A] = new ReportRef[A] {
    def query[B](f: A => B) = f(Reports.this.get[A])
  }

  def get[T <: Report[_]](implicit m: Manifest[T], nn: NotNothing[T]): T = reportByType(m.erasure).get.asInstanceOf[T]

  def applyEvent = event => {
    reportsByEventType.getMostSpecific(event.payload.getClass) foreach {
      updater => updater(event)
    }
  }

  private val reportByType: MMap[Class[_], Ref.View[Report[_]]] = MMap.empty
  private var reportsByEventType: TypeMap[CommittedEvent => Unit] = TypeMap.empty
}
