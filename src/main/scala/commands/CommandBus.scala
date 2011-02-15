package com.zilverline.es2
package commands

import scala.collection.mutable.{Map => MMap}

import events.storage.EventStore

class CommandBus(eventStore: EventStore) {
  def send(command: Command) {
    val handler = handlers.getOrElse(command.getClass, throw new IllegalArgumentException("no handler for found command: " + command))
    val uow = handler.invokeWithCommand(command)(UnitOfWork(None, None))
    if (uow.events.isDefined)
      eventStore.commit(uow.aggregate.get, uow.events.get)
  }

  def register[T <: Command](handler: CommandHandler[T]) = {
    handlers.put(handler.commandType, handler)
  }

  private val handlers: MMap[Class[_], CommandHandler[_]] = MMap.empty
}
