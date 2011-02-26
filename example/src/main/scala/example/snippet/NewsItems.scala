package example.snippet

import example.lib.DependencyFactory
import com.zilverline.es2._
import com.zilverline.es2.commands._
import net.liftweb.util._
import Helpers._
import example.reports.NewsItemReport
import example.events.NewsItemAdded
import net.liftweb.http.{S, SHtml}

class NewsItems {
  lazy val commands = DependencyFactory.commands.vend
  lazy val newsItemsReport = DependencyFactory.reports.vend.queryable[NewsItemReport]

  def list = ".newsitem *" #> newsItemsReport.query(_.newsItems)

  def add = {
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
