package com.zilverline.es2
package commands

import behavior._
import scala.collection.mutable.{Map => MMap}
import eventstore.{Commit, EventStore}
import util.TypeMap

class CommandBus(eventStore: EventStore) {
  def send(command: Command) {
    val handler = handlers.getMostSpecific(command.getClass)
      .getOrElse(throw new IllegalArgumentException("no handler for found command: " + command))

    Behavior.run(handler.invokeWithCommand(command)) match {
      case Reaction(uow, result) =>
        for (source <- uow.tracked.values) {
          eventStore.commit(Commit(source.id, source.revision, source.changes))
        }
    }
  }

  def registerHandler[T <: Command](handler: CommandHandler[T, _]) {
    synchronized {
      handlers += handler.commandType -> handler
    }
  }

  def register[T <: Command](handler: T => Behavior[Any])(implicit m: Manifest[T]) {
    registerHandler(CommandHandler[T](handler))
  }

  @volatile
  private var handlers: TypeMap[CommandHandler[_, _]] = TypeMap.empty
}
