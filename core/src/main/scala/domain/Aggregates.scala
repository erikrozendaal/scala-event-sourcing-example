package com.zilverline.es2
package domain

import scala.collection.mutable.{Map => MMap}

class Aggregates(factories: AggregateFactory*) extends EventProcessor[DomainEvent, Unit] {

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
      val current = get(committedEvent.eventSourceId)
      val expectedSequence = 1L + current.map(_._1).getOrElse(InitialRevision)

      require(committedEvent.sequence <= expectedSequence, "event from the future: " + committedEvent + ", expected sequence " + expectedSequence)

      if (committedEvent.sequence == expectedSequence) {
        val updated = current.map(_._2.internalApplyEvent(committedEvent)).orElse(buildUsingFactory(committedEvent))
        updated.foreach(u => aggregates.put(committedEvent.eventSourceId, (committedEvent.sequence, u)))
      }
    }
  }

  private val aggregates: MMap[Identifier, (Revision, AggregateRoot)] = MMap.empty

  private def buildUsingFactory(committedEvent: CommittedEvent): Option[AggregateRoot] = {
    val initial = factories.find(_.apply(committedEvent.eventSourceId).internalApplyEvent.isDefinedAt(committedEvent))
    initial.map(_.apply(committedEvent.eventSourceId).internalApplyEvent(committedEvent))
  }
}
