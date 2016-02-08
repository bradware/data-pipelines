# Data Pipeline Demo
Repo containing multiple data-pipeline projects using Scala, Kafka 9, and Akka Streams

## Prerequisites

You will need the following things properly installed on your computer.

* [Scala](http://www.scala-lang.org/download)
* [Kafka](http://kafka.apache.org/downloads.html) (v9)

**Note**: These are included in the build.sbt file, however to locally run kafka-topics on your machine you must have kafka-9 source

## Installation

* navigate to where you downloaded kafka 9: `cd kafka_2.11-0.9.0.0`
* Start ZooKeeper server: `bin/zookeeper-server-start.sh config/zookeeper.properties`
* Start Kafka server: `bin/kafka-server-start.sh config/server.properties`
* Create raw data Kafka Topic: `bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name`
* * Create transformed data Kafka Topic: `bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name`
* See new topics created: `bin/kafka-topics.sh --list --zookeeper localhost:2181`

