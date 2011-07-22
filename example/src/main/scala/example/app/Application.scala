package example.app

import org.squeryl.adapters.{MySQLAdapter, H2Adapter}
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.{Session, SessionFactory}
import org.squeryl.PrimitiveTypeMode._
import com.zilverline.es2._, commands._, domain._, eventstore._, reports._
import example.domain._
import example.reports._
import net.liftweb.json.{FullTypeHints, Serialization}
import net.liftweb.util.Props
import net.liftweb.json.ext.JodaTimeSerializers
import org.squeryl.internals.DatabaseAdapter

object Application {
  val eventSerializer = new JsonSerializer()(Serialization.formats(new FullTypeHints(classOf[DomainEvent] :: Nil)) ++ JodaTimeSerializers.all)
  implicit val aggregates = new Aggregates(Invoice)
  val reports = {
    val result = new Reports
    result.register(InvoiceReport())
    result.register(NewsItemReport())
    result
  }
  val invoiceReport = reports.queryable[InvoiceReport]
  val newsItemReport = reports.queryable[NewsItemReport]

  val eventStore: EventStore = {
    val result = new SquerylEventStore(eventSerializer, MultiListenerDispatcher(
        commit => aggregates.applyEvent(commit),
        commit => reports.applyEvent(commit))) with LoggingEventStore
    result
  }
  val commands = new Commands(eventStore, aggregates)

  val jdbcUrl = Props.get("jdbc.url", "jdbc:h2:mem:test")
  val jdbcDriver = Props.get("jdbc.driver", "org.h2.Driver")
  val jdbcUser = Props.get("jdbc.user", "root")
  val jdbcPassword = Props.get("jdbc.password", "")
  val squerylDatabaseAdapterClass = Props.get("squeryl.database.adapter", "org.squeryl.adapters.H2Adapter")

  Class.forName(jdbcDriver)

  val c3p0 = new ComboPooledDataSource
  c3p0.setDriverClass(jdbcDriver)
  c3p0.setJdbcUrl(jdbcUrl)
  c3p0.setUser(jdbcUser)
  c3p0.setPassword(jdbcPassword)
  c3p0.setInitialPoolSize(1)
  c3p0.setMaxPoolSize(100)

  val databaseAdapter = Class.forName(squerylDatabaseAdapterClass).newInstance.asInstanceOf[DatabaseAdapter]

  SessionFactory.concreteFactory = Some {
    () => Session.create(c3p0.getConnection, databaseAdapter)
  }

  try {
    transaction {
      SquerylEventStore.create
    }
  } catch {
    case exception => println("Event store schema creation failed, maybe the tables already exist? " + exception.getMessage)
  }
}
