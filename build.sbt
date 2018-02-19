import Dependencies._

name := "kafka-streams-base"

version := "0.1"

organization := "fr.s3ni0r"

scalaVersion := Versions.Scala_2_12_Version

scalacOptions := Seq("-Xexperimental", "-unchecked", "-deprecation", "-Ywarn-unused-import")

parallelExecution in Test := false

libraryDependencies ++= Seq(
  kafkaStreams excludeAll(ExclusionRule("org.slf4j", "slf4j-log4j12"), ExclusionRule("org.apache.zookeeper", "zookeeper")),
  scalaLogging  % "test",
  logback       % "test",
  kafka         % "test" excludeAll(ExclusionRule("org.slf4j", "slf4j-log4j12"), ExclusionRule("org.apache.zookeeper", "zookeeper")),
  curator       % "test",
  scalaTest     % "test",
  algebird      % "test",
  chill         % "test"
)

testFrameworks += new TestFramework("minitest.runner.Framework")