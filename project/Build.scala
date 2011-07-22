import sbt._, Keys._

object MyBuild extends Build {
  import com.github.siasia.WebPlugin._

  val ScalazVersion = "6.0"
  val LiftVersion = "2.4-M2"
  val Slf4jVersion = "1.6.1"

  val coreDependencies = Seq(
    "org.scala-tools" %% "scala-stm" % "0.3",
    "org.squeryl" %% "squeryl" % "0.9.4" withSources (),
    "org.scalaz" %% "scalaz-core" % ScalazVersion withSources (),
    "net.liftweb" %% "lift-json" % LiftVersion withSources (),
    "net.liftweb" %% "lift-json-ext" % LiftVersion withSources (),
    "org.slf4j" % "slf4j-api" % Slf4jVersion withSources (),
    "junit" % "junit" % "4.8.2" % "test" withSources (),
    "org.specs2" %% "specs2" % "1.3" % "test" withSources (),
    "com.h2database" % "h2" % "1.3.151", // % "test" withSources (),
    "mysql" % "mysql-connector-java" % "5.1.15" % "test"
  )

  val exampleDependencies = Seq(
    // "com.h2database" % "h2" % "1.3.151" % "jetty" withSources (),
    "net.liftweb" %% "lift-webkit" % LiftVersion withSources (),
    //"org.eclipse.jetty" % "jetty-webapp" % "7.2.2.v20101205" % "jetty",
    //"org.eclipse.jetty" % "jetty-webapp" % "7.3.0.v20110203" % "jetty",
    //"org.eclipse.jetty" % "jetty-webapp" % "8.0.0.M2" % "jetty",
    "c3p0" % "c3p0" % "0.9.1.2" % "compile->default" withSources (),
    "ch.qos.logback" % "logback-classic" % "0.9.26"
  )

  override lazy val settings = super.settings ++ Seq(
    name := "es2",
    version := "0.1-SNAPSHOT",
    organization := "com.zilverline",
    scalaVersion := "2.9.0-1",
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    libraryDependencies ++= coreDependencies ++ exampleDependencies
  )

  lazy val root = Project("root", file(".")) aggregate (core, example) settings (name := "es2-root")

  lazy val core = Project("core", file("core")) settings (name := "es2-core")

  lazy val example = Project("example", file("example")) dependsOn (core) settings (webSettings :_*) settings (
    name := "es2-example",
    libraryDependencies ++= Seq(
      "org.mortbay.jetty" % "jetty" % "6.1.25" % "jetty",
      "javax.servlet" % "servlet-api" % "2.5" % "provided"
    )
  )
}