package example.reports

import com.zilverline.es2.reports.Report
import example.events.{NewsItemEvent, NewsItemAdded}

case class NewsItemReport(newsItems: List[String] = Nil) extends Report[NewsItemEvent] {
  def applyEvent = committed => committed.payload match {
    case NewsItemAdded(content) => copy(content :: newsItems)
  }
}
