package example.reports

import com.zilverline.es2._
import com.zilverline.es2.reports.Report
import example.events._

case class InvoiceDocument(invoiceId: Identifier)

case class InvoiceReport(invoices: List[InvoiceDocument] = Nil) extends Report[InvoiceEvent] {
  def applyEvent = committed => committed.payload match {
    case InvoiceCreated() => copy(InvoiceDocument(committed.source) :: invoices)
  }
}
