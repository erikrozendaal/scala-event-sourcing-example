package example.events

trait InvoiceEvent

case class InvoiceCreated() extends InvoiceEvent
