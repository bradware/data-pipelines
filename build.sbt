sbtVersion := "0.13.7"
scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.2",
  "com.twitter" % "hbc-core" % "2.2.0",
  "org.slf4j" % "slf4j-simple" % "1.7.14",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "com.twitter" % "chill_2.10" % "0.7.2"
)
