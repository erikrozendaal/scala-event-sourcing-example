package example.domain

import com.zilverline.es2._
import com.zilverline.es2.domain._
import example.events.{InvoiceCreated, InvoiceEvent}

trait Invoice extends AggregateRoot {
  protected[this] type Event = InvoiceEvent
}

case class InitialInvoice(id: Identifier) extends Invoice {
  def createDraft = created(InvoiceCreated())
  protected[this] def applyEvent = created
  private def created = when[InvoiceCreated] {event => new DraftInvoice(event.source)}
}

case class DraftInvoice(id: Identifier) extends Invoice {
  protected[this] def applyEvent = {
    case event => error("unexpected event: " + event)
  }
}
