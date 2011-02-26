package com.zilverline.es2
package commands

import behavior._
import scala.collection.mutable.{Map => MMap}
import eventstore.{Commit, EventStore}

class CommandBus(eventStore: EventStore) {
  def send(command: Command) {
    val handler = handlers.getOrElse(command.getClass, throw new IllegalArgumentException("no handler for found command: " + command))
    handler.invokeWithCommand(command)(UnitOfWork()) match {
      case Accepted(uow, result) =>
        for (source <- uow.eventSources.values) {
          eventStore.commit(Commit(source.id, source.original, source.changes))
        }
      case Rejected(_) =>
    }
  }

  def register[T <: Command](handler: CommandHandler[T, _, _]) {
    handlers.put(handler.commandType, handler)
  }

  def register[T <: Command](handler: T => Behavior[Any, Any])(implicit m: Manifest[T]) {
    register(CommandHandler(handler))
  }

  private val handlers: MMap[Class[_], CommandHandler[_, _, _]] = MMap.empty
}
