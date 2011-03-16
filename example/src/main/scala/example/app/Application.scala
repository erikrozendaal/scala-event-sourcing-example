package example.app

import org.squeryl.adapters.{MySQLAdapter, H2Adapter}
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.{Session, SessionFactory}
import org.squeryl.PrimitiveTypeMode._
import net.liftweb.json.Serialization
import com.zilverline.es2.commands._
import com.zilverline.es2.domain._
import com.zilverline.es2.reports._
import com.zilverline.es2.eventstore._
import example.commands._
import example.reports._
import example.domain._
import example.model._

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
      command: ChangeInvoiceRecipient =>
        repository.update(command.invoiceId) {
          invoice: DraftInvoice => invoice.changeRecipient(command.recipient)
        }
    }
    result.registerHandler(CommandHandler.updateCommandHandler)
    result
  }

  val jdbcUrl = "jdbc:mysql://localhost:3306/es2"
  val jdbcDriver = "com.mysql.jdbc.Driver"

  Class.forName(jdbcDriver)

  val c3p0 = new ComboPooledDataSource
  c3p0.setDriverClass(jdbcDriver)
  c3p0.setJdbcUrl(jdbcUrl)
  c3p0.setUser("root")
  c3p0.setPassword("")
  c3p0.setInitialPoolSize(100)
  c3p0.setMaxPoolSize(100)

  SessionFactory.concreteFactory = Some {
    () =>
      val result = Session.create(c3p0.getConnection, new MySQLAdapter)
      //result.setLogger(logger.debug _)
      result
  }

  try {
    transaction {
      Product.create
      SquerylEventStore.create
    }
  } catch {
    case exception => println("Event store schema creation failed, maybe the tables already exist? " + exception.getMessage)
  }


}
