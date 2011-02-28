package example.events

sealed trait NewsItemEvent
case class NewsItemAdded(content: String) extends NewsItemEvent
