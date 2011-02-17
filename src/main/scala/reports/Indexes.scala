package com.zilverline.es2
package reports

import scala.collection.mutable.{Map => MMap}

trait Index extends EventProcessor[DomainEvent, Index]

class Indexes {
  def add(index: Index) {
    indexes.put(index.getClass, index)
  }

  def get[T <: Index](implicit m: Manifest[T]): T = indexes(m.erasure).asInstanceOf[T]

  def process(event: CommittedEvent) {
    indexes transform ((_, index) => index.applyEvent(event))
  }

  private val indexes: MMap[Class[_], Index] = MMap.empty
}
