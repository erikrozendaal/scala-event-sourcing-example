package example.commands

import com.zilverline.es2._
import commands.Command

case class CreateDraftInvoice(invoiceId: Identifier) extends Command
