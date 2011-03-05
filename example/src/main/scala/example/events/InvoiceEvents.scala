package example.events

case class InvoiceItem(id: Int, description: String, amount: BigDecimal)

sealed trait InvoiceEvent

case class InvoiceCreated() extends InvoiceEvent
case class InvoiceRecipientChanged(recipient: String, readyToSend: Boolean) extends InvoiceEvent
case class InvoiceItemAdded(item: InvoiceItem, totalAmount: BigDecimal, readyToSend: Boolean) extends InvoiceEvent
