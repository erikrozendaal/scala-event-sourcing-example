package com.zilverline.es2
package examples

import commands._, domain._, eventstore._, transaction._

case class InvoiceItem(id: Int, description: String, amount: BigDecimal)

sealed trait InvoiceEvent
case class InvoiceDraftCreated() extends InvoiceEvent
case class InvoiceRecipientChanged(recipient: Option[String]) extends InvoiceEvent
case class InvoiceItemAdded(item: InvoiceItem, totalAmount: BigDecimal) extends InvoiceEvent

sealed trait InvoiceCommand extends Command
case class CreateDraftInvoice(invoiceId: Identifier) extends InvoiceCommand
case class ChangeInvoiceRecipient(invoiceId: Identifier, recipient: Option[String]) extends InvoiceCommand
case class AddInvoiceItem(invoiceId: Identifier, description: String, price: BigDecimal) extends InvoiceCommand

sealed trait Invoice extends AggregateRoot {
  protected[this] type Event = InvoiceEvent
}

case class InitialInvoice(protected[this] val id: Identifier) extends Invoice {
  def createDraft: Transaction[DraftInvoice] = created(InvoiceDraftCreated())

  protected[this] def applyEvent = created

  private def created = when[InvoiceDraftCreated] {event => DraftInvoice(id)}
}

case class DraftInvoice(
  protected[this] val id: Identifier,
  recipient_? : Boolean = false,
  nextItemId: Int = 1,
  items: Map[Int, InvoiceItem] = Map.empty
  ) extends Invoice {

  def changeRecipient(recipient: Option[String]): Transaction[DraftInvoice] = {
    recipientChanged(InvoiceRecipientChanged(recipient.map(_.trim).filter(_.nonEmpty)))
  }

  def addItem(description: String, amount: BigDecimal): Transaction[DraftInvoice] = {
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
  def applyEvent = _.event match {
    case InvoiceDraftCreated() => this
    case InvoiceRecipientChanged(recipient) =>
      copy(recipient = recipient)
    case InvoiceItemAdded(item, totalAmount) =>
      copy(items = items + (item.id -> item), totalAmount = totalAmount)
  }
}

class InvoiceSpec extends org.specs2.mutable.SpecificationWithJUnit {

  trait Context extends org.specs2.execute.Success {
    val invoiceId = newIdentifier

    val eventStore = new eventstore.MemoryEventStore
    val aggregates = new Aggregates(InitialInvoice)
    eventStore.addListener(committed => aggregates.applyEvent(committed))
    val commands = new CommandBus(eventStore)
    val repository = new AggregateRepository[AggregateRoot](aggregates)

    commands.register[InvoiceCommand] {
      case CreateDraftInvoice(invoiceId) => InitialInvoice(invoiceId).createDraft
      case ChangeInvoiceRecipient(invoiceId, recipient) =>
        repository.update(invoiceId) {invoice: DraftInvoice => invoice.changeRecipient(recipient)}
      case AddInvoiceItem(invoiceId, description, price) =>
        repository.update(invoiceId) {invoice: DraftInvoice => invoice.addItem(description, price)}
    }
  }

  "new invoice" should {
    "be created" in new Context {
      commands.send(CreateDraftInvoice(invoiceId))

      eventStore.load(invoiceId) must contain(Committed(invoiceId, 1, InvoiceDraftCreated()))
    }

    "allow items to be added" in new Context {
      eventStore.commit(Commit(invoiceId, 0, Seq(InvoiceDraftCreated())))

      commands.send(AddInvoiceItem(invoiceId, "beverage", 2.95))
      commands.send(AddInvoiceItem(invoiceId, "sandwich", 4.95))

      eventStore.load(invoiceId) must contain(
        Committed(invoiceId, 2, InvoiceItemAdded(InvoiceItem(1, "beverage", 2.95), 2.95)))
      eventStore.load(invoiceId) must contain(
        Committed(invoiceId, 3, InvoiceItemAdded(InvoiceItem(2, "sandwich", 4.95), 7.90)))
    }
  }
}
