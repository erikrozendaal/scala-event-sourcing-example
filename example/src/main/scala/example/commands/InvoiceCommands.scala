package example.commands

import com.zilverline.es2._
import commands.Command

case class CreateDraftInvoice(invoiceId: Identifier) extends Command
case class ChangeInvoiceRecipient(invoiceId: Identifier, recipient: String) extends Command {
  require(recipient.nonEmpty, "recipient is required")
}
case class AddItemToInvoice(invoiceId: Identifier, description: String, amount: BigDecimal) extends Command {
  require(description.nonEmpty, "description is required")
}
