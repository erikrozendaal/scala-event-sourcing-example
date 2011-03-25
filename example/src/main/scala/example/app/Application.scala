package example.app

import org.squeryl.adapters.{MySQLAdapter, H2Adapter}
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.{Session, SessionFactory}
import org.squeryl.PrimitiveTypeMode._
import com.zilverline.es2._, commands._, domain._, eventstore._, reports._
import example.commands._
import example.reports._
import example.domain._
import example.model._
import net.liftweb.json.{FullTypeHints, Serialization}
import net.liftweb.util.Props

object Application {
  val eventSerializer = new JsonSerializer()(Serialization.formats(new FullTypeHints(classOf[DomainEvent] :: Nil)))
  val aggregates = new Aggregates(Invoice)
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
    result.register[CreateDraftInvoice] {command =>
      Invoice.createDraft(command.invoiceId)
    }
    result.register[ChangeInvoiceRecipient] {command =>
      repository.get[DraftInvoice](command.invoiceId).changeRecipient(command.recipient)
    }
    result.registerHandler(CommandHandler.updateCommandHandler)
    result
  }

  val jdbcUrl = Props.get("jdbc.url", "jdbc:mysql://localhost:3306/es2_dev")
  val jdbcDriver = Props.get("jdbc.driver", "com.mysql.jdbc.Driver")
  val jdbcUser = Props.get("jdbc.user", "root")
  val jdbcPassword = Props.get("jdbc.password", "")

  Class.forName(jdbcDriver)

  val c3p0 = new ComboPooledDataSource
  c3p0.setDriverClass(jdbcDriver)
  c3p0.setJdbcUrl(jdbcUrl)
  c3p0.setUser(jdbcUser)
  c3p0.setPassword(jdbcPassword)
  c3p0.setInitialPoolSize(1)
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
