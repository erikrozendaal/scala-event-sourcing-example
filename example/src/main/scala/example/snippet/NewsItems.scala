package example.snippet

import com.zilverline.es2._
import eventstore.{Commit, EventStore, OptimisticLockingException}
import net.liftweb.util._
import Helpers._
import example.reports.NewsItemReport
import example.events.NewsItemAdded
import net.liftweb.http._, js._, js.JsCmds._
import reports.ReportRef

class NewsItems(eventStore: EventStore, newsItemReport: ReportRef[NewsItemReport])
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
          case s if s.size < 3 => S.error("a news item should contain at least three characters")
          case s =>

            val event = NewsItemAdded(s)
            eventStore.commit(Commit(identifier, InitialRevision, Seq(event)))

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
