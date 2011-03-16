package com.zilverline.es2
package commands

trait Command {
  val commandId = newIdentifier
}

case class Update(eventSourceId: Identifier, revision: Revision, event: DomainEvent) extends Command
