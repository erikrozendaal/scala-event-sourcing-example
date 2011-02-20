package com.zilverline.es2
package reports

import util._

import scala.collection.mutable.{ArrayBuffer, Map => MMap}

trait Index extends EventProcessor[DomainEvent, Index]

class Indexes {
  def add(index: Index) {
    indexByType.put(index.getClass, new AtomicVar(index))
  }

  def get[T <: Index](implicit m: Manifest[T], nn: NotNothing[T]): T = indexByType(m.erasure).get.asInstanceOf[T]

  def process(event: CommittedEvent) {
    indexByType.values.foreach { index => index.modifyWithRetry(_.applyEvent(event)) }
  }

  private val indexByType: MMap[Class[_], AtomicVar[Index]] = MMap.empty
}
