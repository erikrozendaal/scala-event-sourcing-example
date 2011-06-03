package com.zilverline.es2
package domain

import scala.collection.mutable.{Map => MMap}

case class Aggregate(id: Identifier, revision: Revision, root: AggregateRoot)

trait StoresAggregates {
  def get(aggregate: Identifier): Option[Aggregate]
}

trait UpdatesAggregates extends EventProcessor[DomainEvent, Unit]

class Aggregates(factories: AggregateFactory[_]*) extends StoresAggregates with UpdatesAggregates {

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
