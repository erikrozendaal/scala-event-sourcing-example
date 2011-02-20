package example.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import example.lib.DependencyFactory
import example.commands.CreateDraftInvoice
import com.zilverline.es2._
import net.liftweb.util._
import Helpers._
import example.reports.InvoiceReport
import reports.Reports

class Invoices {
  lazy val commands = DependencyFactory.commands.vend
  lazy val reports: Reports = DependencyFactory.reports.vend

  def create = {
    def doSubmit() {
      val command = CreateDraftInvoice(newIdentifier)
      println("sending command: " + command)
      commands.send(command)
    }

    ".submit" #> SHtml.submit("Create invoice", doSubmit)
  }

  def list = {
    val report: InvoiceReport = reports.get[InvoiceReport]
    ".id" #> report.invoices.map(invoice => <p>{invoice}</p>)
  }
}
