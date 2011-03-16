package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import org.squeryl.PrimitiveTypeMode._
import com.zilverline.es2.util.Logging
import example.app._
import example.snippet._
import example.model.Product

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
      example.snippet.Invoices.menu,
      example.snippet.Products.menu)

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

    transaction {
      Product.deleteAll
      Product.insertTestData(36) // 0 -> 2000, 1 -> 2300, 13 -> 3300, 36 -> 9622, 71 -> 17000, 146 -> 33000
    }
    Application.eventStore.replayAllEvents
  }

  import Application._

  LiftRules.snippetDispatch.prepend(Map(
    "Invoices" -> new Invoices(commands, invoiceReport),
    "NewsItems" -> new NewsItems(commands, newsItemReport),
    "Products" -> new Products
  ))
}
