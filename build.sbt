name := """doppelauge"""

version := "1.4.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value

libraryDependencies += specs2 % Test

//libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

scalacOptions in (Compile,doc) := Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-groups",
  "-implicits",
  "-feature",
  "-Xlint",
  "-Xfatal-warnings",
  "-Ywarn-unused-import",
  "-Ywarn-numeric-widen",
  "-Ywarn-dead-code"
)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// Don't generate scaladoc for the "controller" and "router" packages
scalacOptions in (Compile, doc) := List(
  "-skip-packages",  "controllers:router",
  "-doc-source-url", "https://github.com/kmatheussen/Doppelauge/tree/master€{FILE_PATH}.scala"
)

