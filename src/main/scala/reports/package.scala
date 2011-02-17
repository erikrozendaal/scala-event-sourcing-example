package com.zilverline.es2

import events._
import scala.collection.mutable.{Map => MMap}

package object reports {

  type Investigator[T <: DomainEvent] = Committed[T] PartialFunction Document

  trait Index {
    def applyEvent: Committed[AnyRef] => Index
  }

  class Indexes {
    def add(index: Index) {
      indexes.put(index.getClass, index)
    }

    def get[T <: Index](implicit m: Manifest[T]): T = indexes(m.erasure).asInstanceOf[T]

    def process(event: Committed[AnyRef]) {
      indexes transform ((_, index) => index.applyEvent(event))
    }

    private val indexes: MMap[Class[_], Index] = MMap.empty
  }
}
