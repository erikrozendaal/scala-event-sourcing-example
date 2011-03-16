import sbt._

class Project(info: ProjectInfo) extends ParentProject(info) {
  val LiftVersion = "2.2"
  val scalatraVersion = "2.0.0.M3"
  val Slf4jVersion = "1.6.1"

  lazy val core = project("core", "core", new CoreProject(_))
  lazy val example = project("example", "example", new ExampleWebProject(_), core)

  class CoreProject(info: ProjectInfo) extends DefaultProject(info) with growl.GrowlingTests {
    override def compileOptions = super.compileOptions ++ Seq(Unchecked)

    val squeryl = "org.squeryl" % "squeryl_2.8.1" % "0.9.4-RC6" withSources()
    val scalaz = "com.googlecode.scalaz" % "scalaz-core_2.8.0" % "5.0" withSources()
    val liftweb_json = "net.liftweb" % "lift-json_2.8.1" % LiftVersion withSources()
    val slf4j_api = "org.slf4j" % "slf4j-api" % Slf4jVersion withSources()

    val junit = "junit" % "junit" % "4.8.2" % "test" withSources()
    val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test" withSources()
    val scalacheck = "org.scala-tools.testing" % "scalacheck_2.8.1" % "1.8" % "test" withSources()
    val h2 = "com.h2database" % "h2" % "1.3.151" % "test" withSources()
    val mysql = "mysql" % "mysql-connector-java" % "5.1.15" % "test"
    val slf4j_simple = "org.slf4j" % "slf4j-simple" % Slf4jVersion % "test" withSources()
  }

  class ExampleWebProject(info: ProjectInfo) extends DefaultWebProject(info) with org.fusesource.scalate.sbt.PrecompilerWebProject {
    override def scanDirectories = Nil

    override def libraryDependencies = Set(
      "net.liftweb" %% "lift-webkit" % LiftVersion % "compile->default" withSources(),
      "org.scalatra" %% "scalatra" % scalatraVersion % "compile" withSources(),
      "org.scalatra" %% "scalatra-test" % scalatraVersion % "test" withSources(),
      "org.mortbay.jetty" % "jetty" % "6.1.25" % "test->default",
      "javax.servlet" % "servlet-api" % "2.5" % "provided",
      //"org.eclipse.jetty" % "jetty-webapp" % "7.2.2.v20101205" % "test->default",
      //"org.eclipse.jetty" % "jetty-webapp" % "7.3.0.v20110203" % "test->default",
      //"org.eclipse.jetty" % "jetty-webapp" % "8.0.0.M2" % "test->default",
      "c3p0" % "c3p0" % "0.9.1.2" % "compile->default" withSources(),
      "ch.qos.logback" % "logback-classic" % "0.9.26"
    ) ++ super.libraryDependencies
  }

}
