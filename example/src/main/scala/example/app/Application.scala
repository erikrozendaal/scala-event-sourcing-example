package example.app

import org.squeryl.adapters.{MySQLAdapter, H2Adapter}
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.{Session, SessionFactory}
import org.squeryl.PrimitiveTypeMode._
import com.zilverline.es2._, commands._, domain._, eventstore._, reports._
import example.commands._
import example.domain._
import example.reports._
import net.liftweb.json.{FullTypeHints, Serialization}
import net.liftweb.util.Props

object Application {
  val eventSerializer = new JsonSerializer()(Serialization.formats(new FullTypeHints(classOf[DomainEvent] :: Nil)))
  val aggregates = new Aggregates(Invoice)
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
    val result = new CommandBus(eventStore, aggregates)
    result.register[CreateDraftInvoice] {command =>
      Reference[Invoice](command.invoiceId).run(Invoice.createDraft(command.invoiceId))
    }
    result.register[ChangeInvoiceRecipient] {command =>
      Reference[DraftInvoice](command.invoiceId).run(_.changeRecipient(command.recipient))
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

  val databaseAdapter = new MySQLAdapter

  SessionFactory.concreteFactory = Some {
    () =>
      val result = Session.create(c3p0.getConnection, databaseAdapter)
      //result.setLogger(logger.debug _)
      result
  }

  try {
    transaction {
      SquerylEventStore.create
    }
  } catch {
    case exception => println("Event store schema creation failed, maybe the tables already exist? " + exception.getMessage)
  }
}
