package example.reports

import com.zilverline.es2._
import com.zilverline.es2.reports.Report
import example.events._

case class InvoiceDocument(
  invoiceId: Identifier,
  recipient: Option[String] = None,
  readyToSend: Boolean = false,
  totalAmount: BigDecimal = 0)

case class InvoiceReport(
  private val invoices: Map[Identifier, InvoiceDocument] = Map.empty,
  private val recent: List[Identifier] = Nil) extends Report[InvoiceEvent] {

  def mostRecent(n: Int): List[InvoiceDocument] = recent.take(n).map(invoices)

  def applyEvent = committed => {
    val invoiceId = committed.eventSourceId
    committed.event match {
      case InvoiceCreated() =>
        copy(invoices + (invoiceId -> InvoiceDocument(invoiceId)), invoiceId :: recent)

      case InvoiceRecipientChanged(recipient, readyToSend) =>
       update(invoiceId)(_.copy(recipient = Some(recipient), readyToSend = readyToSend))

      case InvoiceItemAdded(_, totalAmount, readyToSend) =>
        update(invoiceId)(_.copy(totalAmount = totalAmount, readyToSend = readyToSend))
    }
  }

  private def update(invoiceId: Identifier)(f: InvoiceDocument => InvoiceDocument) = {
    copy(invoices = invoices.updated(invoiceId, f(invoices(invoiceId))))
  }
}
