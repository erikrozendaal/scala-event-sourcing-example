package com.zilverline.es2
package examples

import org.specs.Specification

import commands._
import commands.CommandHandler._
import events._
import events.storage._

trait InvoiceEvent extends Event {
  def invoiceId: Identifier
}

case class InvoiceCreated(invoiceId: Identifier) extends InvoiceEvent
case class InvoiceRecipientChanged(invoiceId: Identifier, recipient: String) extends InvoiceEvent

case class CreateInvoice(invoiceId: Identifier) extends Command

object InvoiceSpec extends Specification {

  val eventStore = new EventStore
  val bus = new CommandBus(eventStore)

  bus.register(handler {
    command: CreateInvoice => save(command.invoiceId, InvoiceCreated(command.invoiceId))
  })

  "client" should {
    "be able to create invoice" in {
      val invoiceId = newIdentifier

      bus.send(CreateInvoice(invoiceId))

      eventStore.load(invoiceId) must contain(InvoiceCreated(invoiceId))
    }
  }
}
