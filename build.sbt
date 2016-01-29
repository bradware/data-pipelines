sbtVersion := "0.13.7"
scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.2"
)
