package example.snippet

import example.lib.DependencyFactory
import example.commands.CreateDraftInvoice
import com.zilverline.es2._
import net.liftweb.util._
import Helpers._
import example.reports.InvoiceReport
import net.liftweb.http.{S, SHtml}
import net.liftweb.sitemap.Menu

object Invoices {
  val menu = Menu.i("Invoices") / "invoices"
}

class Invoices {
  lazy val commands = DependencyFactory.commands.vend
  lazy val invoiceReport = DependencyFactory.reports.vend.queryable[InvoiceReport]

  def list = ".invoice *" #> invoiceReport.query(_.invoices).take(10).map(_.toString)

  def createDraft = {
    def doSubmit() {
      commands.send(CreateDraftInvoice(newIdentifier))
      S.notice("Invoice created.")
      S.redirectTo(Invoices.menu.path.mkString("/", "/", ""))
    }

    "type=submit" #> SHtml.submit("Create draft invoice", doSubmit)
  }

}
