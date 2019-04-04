enablePlugins(ScalaJSPlugin)

name := "scalachessJs"

version := "1.10"

scalaVersion := "2.12.6"

libraryDependencies ++= List(
  "org.scala-js" %%% "scalajs-dom" % "0.9.6",
  "org.scala-lang.modules" %%% "scala-parser-combinators" % "1.1.0",
  "org.scala-js" %%% "scalajs-java-time" % "0.2.5"
)

resolvers ++= Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Ywarn-infer-any",
  "-Ywarn-dead-code",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard")

emitSourceMaps := false
scalaJSOutputWrapper := ("", "scalachessjs.Main().main();")
