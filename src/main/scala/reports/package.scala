package com.zilverline.es2

package object reports {
  type DocumentFactory[T <: DomainEvent] = Committed[T] PartialFunction Document
}
