ThisBuild / version := "0.1.0-SNAPSHOT"

val ScalaVersion = "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "weather-forecaster",
    scalaVersion := ScalaVersion
  )

val PekkoVersion = "1.0.3"
val LogbackVersion = "1.5.6"

val PekkoHttpVersion = "1.0.1"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-sharding-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
  "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
  "ch.qos.logback" % "logback-classic" % LogbackVersion
)

