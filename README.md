# Data Pipeline Demo
Repo containing multiple data-pipeline projects using Scala, Kafka 9, and Akka Streams. Contains 3 mini-projects:
* Simple Actor Pipeline
* Simple Tweet Pipeline
* Twitter Pipeline

## Technologies

You will need the following things properly installed on your computer.

* [Scala](http://www.scala-lang.org/download)
* [Kafka](http://kafka.apache.org/downloads.html) (v9)
* [Akka Streams](http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0.2/scala.html)
* [Twitter HBC API](https://github.com/twitter/hbc)
* [Kryo - Twitter Chill](https://github.com/twitter/chill)

## Installation

* download [Kafka 9](http://kafka.apache.org/downloads.html)
* navigate to where you downloaded kafka 9: 
  * `cd kafka_2.11-0.9.0.0`
* `bin/zookeeper-server-start.sh config/zookeeper.properties`
* `bin/kafka-server-start.sh config/server.properties`
* Create raw data Kafka Topic (insert real name for topic_name): 
  * `bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name`
* Create transformed data Kafka Topic (insert real name for topic_name: 
  * `bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name`
* See new topics created:
  * `bin/kafka-topics.sh --list --zookeeper localhost:2181`

## Simple Actor Pipeline

1. Publish messages to Kafka Topic through command-line Producer
2. Push messages to an Akka Actor from the Kafka Consumer (which polls from Kafka Topic)
3. Messages go through a actor transition and undergo transformation
4. Final actor dumps output to console

SimpleActor ----> SimpleProcessor -----> SimplePrinter -----> Dumps to console

Run through `SimpleActorPipeline.scala`


