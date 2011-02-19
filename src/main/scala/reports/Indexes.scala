package com.zilverline.es2
package reports

import scala.collection.mutable.{ArrayBuffer, Map => MMap}

trait Index extends EventProcessor[DomainEvent, Index]

class Indexes {
  def add(index: Index) {
    indexByType.put(index.getClass, index)
  }

  def get[T <: Index](implicit m: Manifest[T]): T = indexByType(m.erasure).asInstanceOf[T]

  def process(event: CommittedEvent) {
    indexByType transform ((_, index) => index.applyEvent(event))
  }

  private val indexByType: MMap[Class[_], Index] = MMap.empty
}
