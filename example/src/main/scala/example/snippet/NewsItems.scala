package example.snippet

import com.zilverline.es2._
import com.zilverline.es2.commands._
import net.liftweb.util._
import Helpers._
import example.reports.NewsItemReport
import example.events.NewsItemAdded
import net.liftweb.http._
import reports.QueryableReport

class NewsItems(commands: CommandBus, newsItemReport: QueryableReport[NewsItemReport])
  extends DispatchSnippet {

  def dispatch: DispatchIt = {
    case "list" => list
    case "add" => add
  }

  private def list = ".newsitem *" #> newsItemReport.query(_.newsItems).take(5)

  private def add = {
    var content = ""

    def doSubmit() {
      content = content.trim
      if (content.isEmpty) {
        S.error("content is required")
      } else {
        commands.send(Update(newIdentifier, InitialRevision, NewsItemAdded(content)))
        S.notice("News item '" + content + "' added.")
        S.redirectTo("/")
      }
    }

    "name=content" #> SHtml.onSubmit(content = _) &
      "type=submit" #> SHtml.onSubmitUnit(doSubmit)
  }
}
