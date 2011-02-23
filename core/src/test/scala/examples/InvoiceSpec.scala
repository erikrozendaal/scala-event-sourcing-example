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
  protected[this] type Event = InvoiceEvent
}

object Invoice extends AggregateFactory[Invoice] {
  def create(invoiceId: Identifier): Behavior[Nothing, DraftInvoice] = created(invoiceId, InvoiceCreated())

  protected[this] def applyEvent = created

  private def created = when[InvoiceCreated] {event => DraftInvoice(event.source)}
}

case class DraftInvoice(
  protected[this] val id: Identifier,
  recipient_? : Boolean = false,
  nextItemId: Int = 1,
  items: Map[Int, InvoiceItem] = Map.empty
) extends Invoice {

  def changeRecipient(recipient: Option[String]): Behavior[Nothing, DraftInvoice] = {
    recipientChanged(InvoiceRecipientChanged(recipient.map(_.trim).filter(_.nonEmpty)))
  }

  def addItem(description: String, amount: BigDecimal): Behavior[Nothing, DraftInvoice] = {
    val item = InvoiceItem(nextItemId, description, amount)
    itemAdded(InvoiceItemAdded(item, totalAmount + amount))
  }

  private def totalAmount = items.values.map(_.amount).sum

  protected[this] def applyEvent = recipientChanged orElse itemAdded

  private def recipientChanged = when[InvoiceRecipientChanged] {
    event => copy(recipient_? = event.recipient.isDefined)
  }

  private def itemAdded = when[InvoiceItemAdded] {
    event => copy(items = items + (event.item.id -> event.item), nextItemId = nextItemId + 1)
  }
}

case class InvoiceDocument(
  id: Identifier,
  recipient: Option[String] = None,
  items: Map[Int, InvoiceItem] = Map.empty,
  totalAmount: BigDecimal = 0
) extends EventProcessor[InvoiceEvent, InvoiceDocument] {
  def applyEvent = _.payload match {
    case event: InvoiceRecipientChanged =>
      copy(recipient = event.recipient)
    case event: InvoiceItemAdded =>
      copy(items = items + (event.item.id -> event.item), totalAmount = event.totalAmount)
  }
}

class InvoiceSpec extends Specification {

  val invoiceId = newIdentifier

  val eventStore = new eventstore.MemoryEventStore
  val aggregates = new Aggregates(Invoice)
  eventStore.addListener(committed => aggregates.applyEvent(committed))
  val commands = new CommandBus(eventStore)
  val repository = new AggregateRepository[AggregateRoot](aggregates)

  commands register {
    command: CreateInvoice => Invoice.create(command.invoiceId)
  }
  commands register {
    command: ChangeInvoiceRecipient =>
      repository.get[DraftInvoice](command.invoiceId) flatMap (_.get.changeRecipient(command.recipient))
  }
  commands register {
    command: AddInvoiceItem =>
      repository.get[DraftInvoice](command.invoiceId) flatMap (_.get.addItem(command.description, command.price))
  }

  "client" should {
    "be able to create invoice" in {
      commands.send(CreateInvoice(invoiceId))

      eventStore.load(invoiceId) must contain(Committed(invoiceId, 1, InvoiceCreated()))
    }
  }

  "new invoice" should {
    eventStore.commit(Iterable(Uncommitted(invoiceId, 1, InvoiceCreated())))
    "allow items to be added" in {
      commands.send(AddInvoiceItem(invoiceId, "beverage", 2.95))
      commands.send(AddInvoiceItem(invoiceId, "sandwich", 4.95))

      eventStore.load(invoiceId) must contain(
        Committed(invoiceId, 2, InvoiceItemAdded(InvoiceItem(1, "beverage", 2.95), 2.95)))
      eventStore.load(invoiceId) must contain(
        Committed(invoiceId, 3, InvoiceItemAdded(InvoiceItem(2, "sandwich", 4.95), 7.90)))
    }
  }
}
