package example.domain

import com.zilverline.es2._
import com.zilverline.es2.domain._
import example.events._

trait Invoice extends AggregateRoot {
  protected[this] type Event = InvoiceEvent
}

object Invoice extends AggregateFactory[Invoice] {
  def createDraft(id: Identifier) = created(id, InvoiceCreated())
  protected[this] def applyEvent = created
  private def created = when[InvoiceCreated] {event => new DraftInvoice(event.eventSourceId)}
}

case class DraftInvoice(
  id: Identifier,
  recipient: Option[String] = None,
  nextItemId: Int = 1,
  items: Map[Int, InvoiceItem] = Map.empty)
  extends Invoice {

  def changeRecipient(recipient: String) = recipientChanged(InvoiceRecipientChanged(recipient, items.nonEmpty))

  def addItem(description: String, amount: BigDecimal) = {
    itemAdded(InvoiceItemAdded(InvoiceItem(nextItemId, description, amount), totalAmount + amount, recipient.isDefined))
  }

  private def totalAmount = items.values.map(_.amount).sum

  protected[this] def applyEvent = recipientChanged orElse itemAdded

  private def recipientChanged = when[InvoiceRecipientChanged] {event =>
    copy(recipient = Some(event.recipient))
  }
  private def itemAdded = when[InvoiceItemAdded] {event =>
    copy(nextItemId = event.item.id + 1, items = items + (event.item.id -> event.item))
  }
}
