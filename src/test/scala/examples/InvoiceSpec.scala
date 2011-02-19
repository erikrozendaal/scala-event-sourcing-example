package com.zilverline.es2
package examples

import org.specs.Specification

import behavior._
import commands._
import domain._
import reports._

case class InvoiceItem(id: Int, description: String, amount: BigDecimal)

trait InvoiceEvent
case class InvoiceCreated() extends InvoiceEvent
case class InvoiceRecipientChanged(recipient: Option[String]) extends InvoiceEvent
case class InvoiceItemAdded(item: InvoiceItem, totalAmount: BigDecimal) extends InvoiceEvent

case class CreateInvoice(invoiceId: Identifier) extends Command
case class ChangeInvoiceRecipient(invoiceId: Identifier, recipient: Option[String]) extends Command
case class AddInvoiceItem(invoiceId: Identifier, description: String, price: BigDecimal) extends Command

sealed trait Invoice extends AggregateRoot {
  type Event = InvoiceEvent
}

object Invoice extends AggregateFactory[Invoice] {
  def create(invoiceId: Identifier): Behavior[DraftInvoice] = created(invoiceId, InvoiceCreated())

  def applyEvent = created

  private def created = when[InvoiceCreated] {event => DraftInvoice(event.source)}
}

case class DraftInvoice(
  val id: Identifier,
  recipient_? : Boolean = false,
  nextItemId: Int = 1,
  items: Map[Int, InvoiceItem] = Map.empty
) extends Invoice {

  def changeRecipient(recipient: Option[String]): Behavior[DraftInvoice] = {
    recipientChanged(InvoiceRecipientChanged(recipient.map(_.trim).filter(_.nonEmpty)))
  }

  def addItem(description: String, amount: BigDecimal): Behavior[DraftInvoice] = {
    val item = InvoiceItem(nextItemId, description, amount)
    itemAdded(InvoiceItemAdded(item, totalAmount + amount))
  }

  private def totalAmount = items.values.map(_.amount).sum

  def applyEvent = recipientChanged orElse itemAdded

  private def recipientChanged = when[InvoiceRecipientChanged] {
    event => copy(recipient_? = event.recipient.isDefined)
  }

  private def itemAdded = when[InvoiceItemAdded] {
    case Payload(event) => copy(items = items + (event.item.id -> event.item), nextItemId = nextItemId + 1)
  }
}

case class InvoiceDocument(
  id: Identifier,
  recipient: Option[String] = None,
  items: Map[Int, InvoiceItem] = Map.empty,
  totalAmount: BigDecimal = 0
) extends Document {
  def applyEvent = {
    case Payload(event: InvoiceRecipientChanged) =>
      copy(recipient = event.recipient)
    case Payload(event: InvoiceItemAdded) =>
      copy(items = items + (event.item.id -> event.item), totalAmount = event.totalAmount)
  }
}

class InvoiceReports extends Documents {
  investigate[InvoiceCreated] {
    case Source(invoiceId) => InvoiceDocument(invoiceId)
  }
}

object InvoiceSpec extends Specification {

  implicit val InvoiceFactory = Invoice

  val invoiceId = newIdentifier

  val eventStore = new eventstore.MemoryEventStore
  val commands = new CommandBus(eventStore)
//  val documents = new Documents(eventStore)

  commands register {
    command: CreateInvoice => Invoice.create(command.invoiceId)
  }
  commands register {
    command: ChangeInvoiceRecipient =>
      load[DraftInvoice](command.invoiceId) flatMap (_.changeRecipient(command.recipient))
  }
  commands register {
    command: AddInvoiceItem =>
      load[DraftInvoice](command.invoiceId) flatMap (_.addItem(command.description, command.price))
  }

  "client" should {
    "be able to create invoice" in {
      commands.send(CreateInvoice(invoiceId))

      eventStore.load(invoiceId) must contain(Committed(invoiceId, InvoiceCreated()))
    }
  }

  "new invoice" should {
    eventStore.commit(Iterable(Uncommitted(invoiceId, InvoiceCreated())))
    "allow items to be added" in {
      commands.send(AddInvoiceItem(invoiceId, "beverage", 2.95))
      commands.send(AddInvoiceItem(invoiceId, "sandwich", 4.95))

      eventStore.load(invoiceId) must contain(
        Committed(invoiceId, InvoiceItemAdded(InvoiceItem(1, "beverage", 2.95), 2.95)))
      eventStore.load(invoiceId) must contain(
        Committed(invoiceId, InvoiceItemAdded(InvoiceItem(2, "sandwich", 4.95), 7.90)))
    }
  }
}
