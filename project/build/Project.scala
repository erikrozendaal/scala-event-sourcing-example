import sbt._

class Project(info: ProjectInfo) extends ParentProject(info) {
  val LiftVersion = "2.2"

  lazy val core = project("core", "Core", new CoreProject(_))
  lazy val example = project("example", "Example Web Project", new ExampleWebProject(_), core)

  class CoreProject(info: ProjectInfo) extends DefaultProject(info) with growl.GrowlingTests {
    override def compileOptions = super.compileOptions ++ Seq(Unchecked)

    val squeryl = "org.squeryl" % "squeryl_2.8.1" % "0.9.4-RC3" withSources()
    val scalaz = "com.googlecode.scalaz" % "scalaz-core_2.8.0" % "5.0" withSources()
    val liftweb_json = "net.liftweb" % "lift-json_2.8.1" % LiftVersion withSources()

    val junit = "junit" % "junit" % "4.8.2" % "test" withSources()
    val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test" withSources()
    val scalacheck = "org.scala-tools.testing" % "scalacheck_2.8.1" % "1.8" % "test" withSources()
    val h2 = "com.h2database" % "h2" % "1.3.151" % "test" withSources()
  }

  class ExampleWebProject(info: ProjectInfo) extends DefaultWebProject(info) {
    override def scanDirectories = Nil

    override def libraryDependencies = Set(
      "net.liftweb" %% "lift-webkit" % LiftVersion % "compile->default" withSources(),
      "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default",
      "ch.qos.logback" % "logback-classic" % "0.9.26"
    ) ++ super.libraryDependencies
  }

}
