package example.commands

import com.zilverline.es2._
import commands.Command

trait InvoiceCommand extends Command {
  val invoiceId: Identifier
}
case class CreateDraftInvoice(invoiceId: Identifier) extends InvoiceCommand
case class ChangeInvoiceRecipient(invoiceId: Identifier, recipient: String) extends InvoiceCommand
case class AddItemToInvoice(invoiceId: Identifier, description: String, amount: BigDecimal) extends InvoiceCommand
