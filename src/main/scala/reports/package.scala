package com.zilverline.es2

package object reports {
  type Investigator[T <: DomainEvent] = Committed[T] PartialFunction Document
}
