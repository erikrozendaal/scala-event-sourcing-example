package example {
package lib {

import net.liftweb._
import http._
import json.Serialization
import util._
import common._
import _root_.java.util.Date
import com.zilverline.es2.reports.Reports
import example.domain._
import commands._
import reports.{NewsItemReport, InvoiceReport}
import com.zilverline.es2.commands.{CommandHandler, CommandBus}
import com.zilverline.es2.domain.{AggregateRepository, Aggregates}
import com.zilverline.es2.eventstore._

/**
 * A factory for generating new instances of Date.  You can create
 * factories for each kind of thing you want to vend in your application.
 * An example is a payment gateway.  You can change the default implementation,
 * or override the default implementation on a session, request or current call
 * stack basis.
 */
object DependencyFactory extends Factory {
  implicit object time extends FactoryMaker(Helpers.now _)

  lazy val eventSerializer = new JsonSerializer()(Serialization.formats(new ReflectionTypeHints))
  lazy val aggregates = new Aggregates(InitialInvoice)
  lazy val repository = new AggregateRepository[Invoice](aggregates)
  lazy val reports = {
    val result = new Reports
    result.register(InvoiceReport())
    result.register(NewsItemReport())
    result
  }
  lazy val eventStore: EventStore = {
    val result = new SquerylEventStore(eventSerializer) with LoggingEventStore
    result.addListener(commit => aggregates.applyEvent(commit))
    result.addListener(commit => reports.applyEvent(commit))
    result
  }
  lazy val commands = {
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

  /**
   * objects in Scala are lazily created.  The init()
   * method creates a List of all the objects.  This
   * results in all the objects getting initialized and
   * registering their types with the dependency injector
   */
  private def init() {
    List(time, eventStore)
  }
  init()
}

/*
/**
 * Examples of changing the implementation
 */
sealed abstract class Changer {
  def changeDefaultImplementation() {
    DependencyFactory.time.default.set(() => new Date())
  }

  def changeSessionImplementation() {
    DependencyFactory.time.session.set(() => new Date())
  }

  def changeRequestImplementation() {
    DependencyFactory.time.request.set(() => new Date())
  }

  def changeJustForCall(d: Date) {
    DependencyFactory.time.doWith(d) {
      // perform some calculations here
    }
  }
}
*/
}
}
