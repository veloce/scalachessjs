enablePlugins(ScalaJSPlugin)

name := "ScalachessJs"

version := "1.1"

scalaVersion := "2.11.7"

libraryDependencies ++= List(
  "org.scala-js" %%% "scalajs-dom" % "0.9.0",
  "org.specs2" %% "specs2-core" % "3.6" % "test",
  "joda-time" % "joda-time" % "2.9.2",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0",
  "org.scala-js" %%% "scala-parser-combinators" % "1.0.2"
)

resolvers ++= Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8")

emitSourceMaps := false
scalaJSOutputWrapper := ("", "scalachessjs.Main().main();")
