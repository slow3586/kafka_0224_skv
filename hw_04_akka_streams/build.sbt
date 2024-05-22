ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "hw_04_akka_streams"
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.2"
)