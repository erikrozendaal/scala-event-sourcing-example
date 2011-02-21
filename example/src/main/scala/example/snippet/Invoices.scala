package example.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import example.lib.DependencyFactory
import example.commands.CreateDraftInvoice
import com.zilverline.es2._
import net.liftweb.util._
import Helpers._
import example.reports.InvoiceReport

class Invoices {
  lazy val commands = DependencyFactory.commands.vend
  lazy val invoiceReport = DependencyFactory.reports.vend.queryable[InvoiceReport]

  def create = {
    def doSubmit() {
      val command = CreateDraftInvoice(newIdentifier)
      println("sending command: " + command)
      commands.send(command)
    }

    ".submit" #> SHtml.submit("Create invoice", doSubmit)
  }

  def list = {
    val invoices = invoiceReport.query(_.invoices.map(invoice => <p>{invoice}</p>))
    ".id" #> invoices
  }
}
