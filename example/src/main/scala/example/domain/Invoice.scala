package example.domain

import example.events._
import com.zilverline.es2.domain._, Behavior._
import org.joda.time.LocalDate

sealed trait Invoice extends AggregateRoot {
  protected[this] type Event = InvoiceEvent
}

object Invoice extends AggregateFactory[Invoice] {
  def createDraft: Behavior[DraftInvoice] = created(InvoiceCreated())

  protected[this] def applyEvent = created

  private def created = when[InvoiceCreated] {event => DraftInvoice()}
}

case class DraftInvoice(
  recipient: Option[String] = None,
  nextItemId: Int = 1,
  items: Map[Int, InvoiceItem] = Map.empty) extends Invoice {

  def changeRecipient(recipient: String): Behavior[DraftInvoice] =
    recipientChanged(InvoiceRecipientChanged(recipient, readyToSend_?))

  def addItem(description: String, amount: Double): Behavior[DraftInvoice] =
    itemAdded(InvoiceItemAdded(InvoiceItem(nextItemId, description, amount), totalAmount + amount, readyToSend_?))

  def send(sentOn: LocalDate): Behavior[Either[String, SentInvoice]] =
    if (readyToSend_?)
      for (result <- sent(InvoiceSent(sentOn, paymentDueOn = sentOn.plusDays(14)))) yield Right(result)
    else
      pure(Left("invoice not ready to send"))

  private def totalAmount = items.values.map(_.amount).sum

  private def readyToSend_? = recipient.isDefined && items.nonEmpty

  protected[this] def applyEvent = recipientChanged orElse itemAdded orElse sent

  private def recipientChanged = when[InvoiceRecipientChanged] {
    event => copy(recipient = Some(event.recipient))
  }
  private def itemAdded = when[InvoiceItemAdded] {
    event => copy(nextItemId = event.item.id + 1, items = items + (event.item.id -> event.item))
  }
  private def sent = when[InvoiceSent] {
    event => SentInvoice(event.sentOn, event.paymentDueOn)
  }
}

case class SentInvoice(sentOn: LocalDate, paymentDueOn: LocalDate) extends Invoice {
  protected[this] def applyEvent = sys.error("final state")
}
