package example {
package lib {

import net.liftweb._
import http._
import json.Serialization
import util._
import common._
import _root_.java.util.Date
import com.zilverline.es2.domain.Aggregates
import com.zilverline.es2.eventstore.{ReflectionTypeHints, JsonSerializer, SquerylEventStore}
import com.zilverline.es2.reports.Reports
import example.domain._
import com.zilverline.es2.commands.CommandBus
import commands.CreateDraftInvoice
import reports.InvoiceReport

/**
 * A factory for generating new instances of Date.  You can create
 * factories for each kind of thing you want to vend in your application.
 * An example is a payment gateway.  You can change the default implementation,
 * or override the default implementation on a session, request or current call
 * stack basis.
 */
object DependencyFactory extends Factory {
  implicit object time extends FactoryMaker(Helpers.now _)

  implicit object eventSerializer extends FactoryMaker(new JsonSerializer()(Serialization.formats(new ReflectionTypeHints)))
  implicit object aggregates extends FactoryMaker(new Aggregates(InitialInvoice))
  implicit object reports extends FactoryMaker({
    val result = new Reports
    result.register(InvoiceReport())
    result
  })
  implicit object eventStore extends FactoryMaker({
    val result = new SquerylEventStore(eventSerializer.vend)
    result.addListener(commit => aggregates.vend.applyEvent(commit))
    result.addListener(commit => reports.vend.applyEvent(commit))
    result
  })
  implicit object commands extends FactoryMaker({
    val result = new CommandBus(eventStore.vend)
    result register {
      command: CreateDraftInvoice => InitialInvoice(command.invoiceId).createDraft
    }
    result
  })

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
