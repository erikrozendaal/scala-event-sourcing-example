import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) with growl.GrowlingTests {
  val squeryl = "org.squeryl" % "squeryl_2.8.1" % "0.9.4-RC3" withSources()
  val scalaz = "com.googlecode.scalaz" % "scalaz-core_2.8.0" % "5.0" withSources()
  val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test" withSources()
  val scalacheck = "org.scala-tools.testing" % "scalacheck_2.8.1" % "1.8" % "test" withSources()
}
