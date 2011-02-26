package example.events

trait NewsItemEvent
case class NewsItemAdded(content: String) extends NewsItemEvent
