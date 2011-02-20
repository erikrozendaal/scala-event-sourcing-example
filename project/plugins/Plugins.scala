import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val lessRepo = "lessis repo" at "http://repo.lessis.me"
  val growl = "me.lessis" % "sbt-growl-plugin" % "0.0.5"

  val sbtIdeaRepo = "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
  val sbtIdea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.3.0"
}
