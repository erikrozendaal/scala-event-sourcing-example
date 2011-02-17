package com.zilverline.es2

trait EventProcessor[A <: DomainEvent, +B] {
  def applyEvent: Committed[A] PartialFunction B
}

object EventProcessor {
  def apply[A <: DomainEvent, B](callback: Committed[A] PartialFunction B)(implicit m: Manifest[A]): EventProcessor[A, B] = new EventProcessor[A, B] {
    def applyEvent = callback
  }
}
