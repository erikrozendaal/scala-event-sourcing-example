package com.zilverline.es2

import behavior._

package object domain {
  def load[AR <: AggregateRoot](factory: AggregateFactory[_], source: Identifier): Behavior[AR] = Behavior {
    uow =>
      val events = uow.eventStore.load(source)
      Accepted(uow, factory.loadFromHistory(events))
  }
}
