package example.events

sealed trait InvoiceEvent
case class InvoiceCreated() extends InvoiceEvent
