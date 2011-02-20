package com.zilverline.es2
package domain

import scala.collection.mutable.{Map => MMap}

class Aggregates(factories: AggregateFactory[_]*) extends EventProcessor[DomainEvent, Unit] {

  def get(aggregate: Identifier): Option[(Revision, AggregateRoot)] = {
    aggregates synchronized {aggregates.get(aggregate)}
  }

  def putIfNewer(aggregate: AggregateRoot, revision: Revision) {
    aggregates synchronized {
      val (current, _) = aggregates.getOrElseUpdate(aggregate.internalId, (revision, aggregate))
      if (revision > current)
        aggregates.put(aggregate.internalId, (revision, aggregate))
    }
  }

  def applyEvent = committedEvent => {
    aggregates synchronized {
      val current = get(committedEvent.source)
      val expectedRevision = current.map(_._1 + 1L).getOrElse(InitialRevision)

      require(committedEvent.sequence <= expectedRevision, "event from the future: " + committedEvent + ", expected revision " + expectedRevision)

      if (committedEvent.sequence == expectedRevision) {
        val updated = current.map(_._2.internalApplyEvent(committedEvent)).getOrElse(buildUsingFactory(committedEvent))
        aggregates.put(committedEvent.source, (committedEvent.sequence, updated))
      }
    }
  }

  private val aggregates: MMap[Identifier, (Revision, AggregateRoot)] = MMap.empty

  private def buildUsingFactory(committedEvent: CommittedEvent): AggregateRoot = {
    val factory = factories.find(_.internalApplyEvent.isDefinedAt(committedEvent)).getOrElse(error("no factory for event " + committedEvent))
    factory.internalApplyEvent(committedEvent)
  }
}
