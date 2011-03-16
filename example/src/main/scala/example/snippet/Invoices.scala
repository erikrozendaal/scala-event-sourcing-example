package example.snippet

import example.commands._
import com.zilverline.es2._, commands.CommandBus, reports.QueryableReport
import net.liftweb.util._
import Helpers._
import example.reports.InvoiceReport
import java.util.UUID
import net.liftweb.sitemap._
import net.liftweb.http._

object Invoices {
  import Loc._

  val menu = Menu(
    Loc("invoices", "invoices" :: Nil, "Invoices"),
    Menu(Loc("edit-invoice", "invoices" :: "edit" :: Nil, "Edit invoice", Hidden)))
}

class Invoices(commands: CommandBus, invoiceReport: QueryableReport[InvoiceReport]) extends DispatchSnippet {
  def dispatch: DispatchIt = {
    case "list" => list
    case "createDraft" => createDraft
    case "editRecipient" => editRecipient
  }

  private def list = ".invoice" #> invoiceReport.query(_.mostRecent(S.param("n").map(_.toInt).openOr(10))).map(invoice =>
    <tr><td>{invoice.invoiceId}</td><td>{invoice.recipient.getOrElse("")}</td><td><a href={"/invoices/edit?invoice=" + invoice.invoiceId}>edit</a></td></tr>)

  private def createDraft = {
    def doSubmit() {
      val invoiceId = newIdentifier
      commands.send(CreateDraftInvoice(invoiceId))
      S.notice("Invoice created.")
      S.redirectTo("/invoices/edit?invoice=" + invoiceId)
    }

    "type=submit" #> SHtml.submit("Create draft invoice", doSubmit)
  }

  private def editRecipient = {
    val uri = S.uriAndQueryString openOr "/"
    val invoiceId = UUID.fromString(S.param("invoice").open_!)
    var recipient = ""

    def doSubmit() {
      recipient = recipient.trim
      if (recipient.isEmpty) {
        S.error("recipient cannot be empty")
      } else {
        commands.send(ChangeInvoiceRecipient(invoiceId, recipient))
        S.notice("Recipient changed to '" + recipient + "'.")
      }
      S.redirectTo(uri)
    }

    "name=recipient" #> SHtml.onSubmit(recipient = _) &
      "type=submit" #> SHtml.onSubmitUnit(doSubmit)
  }
}
