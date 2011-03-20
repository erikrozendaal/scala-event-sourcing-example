package example.snippet

import com.zilverline.es2._
import com.zilverline.es2.commands._
import eventstore.OptimisticLockingException
import net.liftweb.util._
import Helpers._
import example.reports.NewsItemReport
import example.events.NewsItemAdded
import net.liftweb.http._, js._, js.JsCmds._
import reports.QueryableReport

class NewsItems(commands: CommandBus, newsItemReport: QueryableReport[NewsItemReport])
  extends SimpleStateful {

  private var identifier = newIdentifier
  private var content = ""

  def render = {
    "div" #> SHtml.idMemoize(html => (list & add) andThen ("form *+" #> SHtml.hidden(html.setHtml _)))
  }

  private def list = ".newsitem *" #> newsItemReport.query(_.newsItems).take(5)

  private def add = {
    def doSubmit() {
      try {
        content.trim match {
          case s if s.size < 3 => S.error("content is required")
          case s =>
            commands.send(Update(identifier, InitialRevision, NewsItemAdded(s)))
            S.notice("News item '" + s + "' added.")
            identifier = newIdentifier
            content = ""
        }
      } catch {
        case e: OptimisticLockingException => S.error("optimistic locking exception")
      }
    }

    "name=content" #> (FocusOnLoad(SHtml.text(content, content = _)) ++ SHtml.hidden(doSubmit _))
  }
}
