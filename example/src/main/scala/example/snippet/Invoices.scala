package example.snippet

import example.commands._
import com.zilverline.es2._, commands.CommandBus, reports.QueryableReport
import net.liftweb.util._
import Helpers._
import java.util.UUID
import net.liftweb.sitemap._
import net.liftweb.http._, js.JsCmds.FocusOnLoad
import example.reports.{InvoiceDocument, InvoiceReport}
import example.app.Application
import xml.NodeSeq

object Invoices {

  lazy val invoiceReport = Application.invoiceReport

  val editInvoiceMenu = Menu.param[InvoiceDocument]("edit-invoice", "Edit",
    id => invoiceReport.query(_.get(Identifier.fromString(id))),
    _.invoiceId.toString) / "invoices" / * / "edit" >> Loc.Hidden

  lazy val editInvoiceLoc = editInvoiceMenu.toLoc

  val menu = Menu("list-invoices", "Invoices") / "invoices" / "index" >> Loc.Stateless submenus (editInvoiceMenu)

  val listInvoicesLink = menu.loc.createDefaultLink.get.text
}

class Invoices(commands: CommandBus, invoiceReport: QueryableReport[InvoiceReport]) extends DispatchSnippet {
  def dispatch: DispatchIt = {
    case "list" => list
    case "createDraft" => createDraft
  }

  private def list = {
    val count = S.param("n").flatMap(asInt).openOr(10)
    val invoices = invoiceReport.query(_.mostRecent(count))

    ".invoice *" #> invoices.map {
      invoice =>
        ".number *" #> "unassigned" &
          ".recipient *" #> invoice.recipient.getOrElse("") &
          ".totalAmount *" #> "€ %.2f".format(invoice.totalAmount) &
          ".actions *" #> <a href={Invoices.editInvoiceLoc.calcHref(invoice)}>edit</a>
    }
  }

  private def createDraft: NodeSeq => NodeSeq = {
    if (S.post_?) {
      val invoiceId = newIdentifier
      commands.send(CreateDraftInvoice(invoiceId))
      S.notice("Draft invoice created.")
      S.redirectTo(Invoices.editInvoiceLoc.calcHref(InvoiceDocument(invoiceId)))
    }

    identity
  }
}

class EditInvoiceSnippet(commands: CommandBus) extends SimpleStateful {
  def render = {
    val invoice = Invoices.editInvoiceLoc.currentValue.open_!
    var recipient = invoice.recipient.getOrElse("")

    def doSubmit() {
      recipient = recipient.trim
      if (recipient.isEmpty) {
        S.error("The recipient must be specified")
      } else {
        commands.send(ChangeInvoiceRecipient(invoice.invoiceId, recipient))
        S.notice("Recipient changed to '" + recipient + "'.")
        S.redirectTo(Invoices.listInvoicesLink)
      }
    }

    "@recipient" #> FocusOnLoad(SHtml.text(recipient, recipient = _)) &
      ":submit" #> SHtml.onSubmitUnit(doSubmit)
  }
}
