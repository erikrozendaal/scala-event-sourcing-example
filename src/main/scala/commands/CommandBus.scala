package com.zilverline.es2
package commands

import behavior._
import eventstore.EventStore

import scala.collection.mutable.{Map => MMap}

class CommandBus(eventStore: EventStore) {
  def send(command: Command) {
    val handler = handlers.getOrElse(command.getClass, throw new IllegalArgumentException("no handler for found command: " + command))
    handler.invokeWithCommand(command)(UnitOfWork(Nil, eventStore)) match {
      case Accepted(uow, result) => eventStore.commit(uow.events)
      case Rejected(message) =>
    }
  }

  def register[T <: Command](handler: CommandHandler[T, _]) = {
    handlers.put(handler.commandType, handler)
  }

  private val handlers: MMap[Class[_], CommandHandler[_, _]] = MMap.empty
}
