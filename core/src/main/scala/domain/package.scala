package com.zilverline.es2

package object domain {
  type AggregateFactory = Identifier => AggregateRoot

  def loadFromHistory[AR <: AggregateRoot](factory: AggregateFactory)(history: Iterable[RecordedEvent]): AR = {
    val aggregate = factory(history.head.source)
    (aggregate /: history)(_.internalApplyEvent(_)).asInstanceOf[AR]
  }
}
