package example.domain

import com.zilverline.es2._
import com.zilverline.es2.domain._
import example.events.{InvoiceCreated, InvoiceEvent}

case class Invoice(id: Identifier) extends AggregateRoot {
  protected[this] type Event = InvoiceEvent

  protected[this] def applyEvent = {
    case event => error("unexpected event: " + event)
  }
}

object Invoice extends AggregateFactory[Invoice] {
  def createDraft(id: Identifier) = created(id, InvoiceCreated())
  protected[this] def applyEvent = created
  private def created = when[InvoiceCreated] {event => new Invoice(event.source)}
}
