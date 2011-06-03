package example.reports

import com.zilverline.es2._
import com.zilverline.es2.reports.Report
import example.events._
import collection.immutable.SortedMap
import org.joda.time.LocalDate

case class InvoiceDocument(
  invoiceId: Identifier,
  recipient: Option[String] = None,
  readyToSend: Boolean = false,
  totalAmount: Double = 0,
  sentOn: Option[LocalDate] = None,
  paymentDueOn: Option[LocalDate] = None)

case class InvoiceReport(
  private val invoices: SortedMap[Identifier, InvoiceDocument] = SortedMap.empty,
  private val recent: List[Identifier] = Nil) extends Report[InvoiceEvent] {

  def mostRecent(n: Int): List[InvoiceDocument] = recent.take(n).map(invoices)

  def get(id: Identifier) = invoices.get(id)

  def applyEvent = committed => {
    val invoiceId = committed.eventSourceId
    committed.payload match {
      case InvoiceCreated() =>
        copy(invoices + (invoiceId -> InvoiceDocument(invoiceId)), invoiceId :: recent)

      case InvoiceRecipientChanged(recipient, readyToSend) =>
       update(invoiceId)(_.copy(recipient = Some(recipient), readyToSend = readyToSend))

      case InvoiceItemAdded(_, totalAmount, readyToSend) =>
        update(invoiceId)(_.copy(totalAmount = totalAmount, readyToSend = readyToSend))

      case InvoiceSent(sentOn, paymentDueOn) =>
        update(invoiceId)(_.copy(sentOn = Some(sentOn), paymentDueOn = Some(paymentDueOn)))
    }
  }

  private def update(invoiceId: Identifier)(f: InvoiceDocument => InvoiceDocument) = {
    copy(invoices = invoices.updated(invoiceId, f(invoices(invoiceId))))
  }
}
