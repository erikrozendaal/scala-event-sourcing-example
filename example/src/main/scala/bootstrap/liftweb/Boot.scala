package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import org.squeryl.{Session, SessionFactory}
import org.squeryl.adapters.H2Adapter
import org.squeryl.PrimitiveTypeMode._
import com.zilverline.es2.eventstore.SquerylEventStore
import example.lib.DependencyFactory
import com.zilverline.es2.util.Logging

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Logging {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("example")

    // Build SiteMap
    val entries = List(
      Menu.i("News Items") / "index", // the simple way to declare a menu
      example.snippet.Invoices.menu)

      // more complex because this menu allows anything in the
      // /static path to be visible
//      Menu(Loc("Static", Link(List("static"), true, "/static/index"),
//	       "Static Content")))

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Squeryl for the event store
    val jdbcUrl = "jdbc:h2:test"
    val jdbcDriver = "org.h2.Driver"

    Class.forName(jdbcDriver)

    SessionFactory.concreteFactory = Some {
      () =>
        val result = Session.create(java.sql.DriverManager.getConnection(jdbcUrl), new H2Adapter)
        result.setLogger(logger.debug _)
        result
    }

    try {
      transaction {
        SquerylEventStore.create
      }
    } catch {
      case exception => println("Event store schema creation failed, maybe the tables already exist? " + exception.getMessage)
    }

    DependencyFactory.eventStore.vend.replayAllEvents
  }
}
