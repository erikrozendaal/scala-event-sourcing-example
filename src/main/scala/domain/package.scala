package com.zilverline.es2

import behavior._

package object domain {
  def load[AR <: AggregateRoot](source: Identifier)(implicit factory: AggregateFactory[AR]): Behavior[AR] = Behavior {
    uow =>
      val events = uow.eventStore.load(source)
      val aggregate = factory.loadFromHistory[AR](events)
      Accepted(uow.trackEventSource(source, events.lastOption.map(_.sequence).getOrElse(0L), aggregate), aggregate)
  }
}
