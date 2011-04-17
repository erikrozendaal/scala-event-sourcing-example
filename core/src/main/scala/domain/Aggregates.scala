package com.zilverline.es2
package domain

import scala.collection.mutable.{Map => MMap}

case class Aggregate(id: Identifier, revision: Revision, root: AggregateRoot)

class Aggregates(factories: AggregateFactory[_]*) extends EventProcessor[DomainEvent, Unit] {

  def apply(aggregate: Identifier): Aggregate = aggregates synchronized {
    aggregates.getOrElse(aggregate, error("unknown aggregate <" + aggregate + ">"))
  }

  def get(aggregate: Identifier): Option[Aggregate] = aggregates synchronized {
    aggregates.get(aggregate)
  }

  def applyEvent = committedEvent => {
    aggregates synchronized {
      val current = get(committedEvent.eventSourceId)
      val expectedSequence = 1L + current.map(_.revision).getOrElse(InitialRevision)

      require(committedEvent.sequence <= expectedSequence, "event from the future: " + committedEvent + ", expected sequence " + expectedSequence)

      if (committedEvent.sequence == expectedSequence) {
        val updated = current.map(_.root.internalApplyEvent(committedEvent)).orElse(buildUsingFactory(committedEvent))
        updated.foreach(u => aggregates.put(committedEvent.eventSourceId, Aggregate(committedEvent.eventSourceId, committedEvent.sequence, u)))
      }
    }
  }

  private val aggregates: MMap[Identifier, Aggregate] = MMap.empty

  private def buildUsingFactory(event: CommittedEvent): Option[AggregateRoot] = {
    val initial = factories.find(_.internalApplyEvent.isDefinedAt(event))
    initial.map(_.internalApplyEvent(event))
  }
}
