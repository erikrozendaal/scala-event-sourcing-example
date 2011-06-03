package example.events

import org.joda.time.LocalDate

case class InvoiceItem(id: Int, description: String, amount: Double)

sealed trait InvoiceEvent
case class InvoiceCreated() extends InvoiceEvent
case class InvoiceRecipientChanged(recipient: String, readyToSend: Boolean) extends InvoiceEvent
case class InvoiceItemAdded(item: InvoiceItem, totalAmount: Double, readyToSend: Boolean) extends InvoiceEvent
case class InvoiceSent(sentOn: LocalDate, paymentDueOn: LocalDate) extends InvoiceEvent
