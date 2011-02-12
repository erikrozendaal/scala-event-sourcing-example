package com.zilverline.es2

package object indexing {

  import scala.collection.mutable.{Map => MMap}
  import eventing._

  trait Index {
    def applyEvent: Event => Index
  }

  class Indexes {
    def add(index: Index) {
      indexes.put(index.getClass, index)
    }

    def get[T <: Index](implicit m: Manifest[T]): T = indexes(m.erasure).asInstanceOf[T]

    def process(event: Event) {
      indexes transform ((_, index) => index.applyEvent(event))
    }

    private val indexes: MMap[Class[_], Index] = MMap.empty
  }

}
