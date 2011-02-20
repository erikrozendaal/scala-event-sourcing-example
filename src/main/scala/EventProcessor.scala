package com.zilverline.es2

trait EventProcessor[-A <: DomainEvent, +B] {
  def applyEvent: Committed[A] => B
}
