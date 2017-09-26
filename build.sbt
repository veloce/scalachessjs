enablePlugins(ScalaJSPlugin)

name := "scalachessJs"

version := "1.8"

scalaVersion := "2.11.8"

libraryDependencies ++= List(
  "org.scala-js" %%% "scalajs-dom" % "0.9.1",
  "org.specs2" %% "specs2-core" % "3.6" % "test",
  "joda-time" % "joda-time" % "2.9.7",
  "org.scala-js" %%% "scala-parser-combinators" % "1.0.2",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
)

resolvers ++= Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Ywarn-unused-import", "-Ywarn-value-discard", "-Ywarn-dead-code",
  "-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8")

emitSourceMaps := false
scalaJSOutputWrapper := ("", "scalachessjs.Main().main();")
