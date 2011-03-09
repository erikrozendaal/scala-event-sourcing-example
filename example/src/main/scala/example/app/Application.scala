package example.app

import net.liftweb.json.Serialization
import com.zilverline.es2.commands._
import com.zilverline.es2.domain._
import com.zilverline.es2.reports._
import com.zilverline.es2.eventstore._
import example.commands._
import example.reports._
import example.domain._

object Application {
  val eventSerializer = new JsonSerializer()(Serialization.formats(new ReflectionTypeHints))
  val aggregates = new Aggregates(InitialInvoice)
  val repository = new AggregateRepository[Invoice](aggregates)
  val reports = {
    val result = new Reports
    result.register(InvoiceReport())
    result.register(NewsItemReport())
    result
  }
  val invoiceReport = reports.queryable[InvoiceReport]
  val newsItemReport = reports.queryable[NewsItemReport]

  val eventStore: EventStore = {
    val result = new SquerylEventStore(eventSerializer) with LoggingEventStore
    result.addListener(commit => aggregates.applyEvent(commit))
    result.addListener(commit => reports.applyEvent(commit))
    result
  }
  val commands = {
    val result = new CommandBus(eventStore)
    result register {
      command: CreateDraftInvoice => InitialInvoice(command.invoiceId).createDraft
    }
    result register {
      command: ChangeInvoiceRecipient => repository.update(command.invoiceId) {
        invoice: DraftInvoice => invoice.changeRecipient(command.recipient)
      }
    }
    result.registerHandler(CommandHandler.updateCommandHandler)
    result
  }
}
