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

## Getting Started
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
* Publish messages to Kafka Topic through command-line **Kafka Producer**
  *  `bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topic_name`
* Push messages to an Akka Actor from the Kafka Consumer (which polls from Kafka Topic)
* Messages go through a actor transition and undergo transformation
* Final actor dumps output to console

`SimpleActor ----> SimpleProcessor -----> SimplePrinter -----> Dumps to console`

**Note**: Make sure the kakfa topic in `SimpleActorPipeline.scala` matches the one you created during **Getting Started** and for the command line **Kafka Producer**

Run through `SimpleActorPipeline.scala`

## Simple Tweet Pipeline
* Publish messages to Kafka Topic through command-line **Kafka Producer**
  *  `bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topic_name`
* Pull messages from Kafka Consumer into Akka ActorPublisher
* Push Messages through an Akka Stream/Runnable Flow and undergo transformation (Source)
* Subscriber reads the messages from the Akka Stream/Runnable Flow (Sink)
* Subscriber/Sink dumps the transformed to the console

**Note**: Make sure the kakfa topic in `SimpleActorPipeline.scala` matches the one you created during **Getting Started**

Run through `SimpleTweetPipeline.scala`

## Twitter Pipeline
In `Config.scala` update the terms list to apply the correct Twitter filtering for your pipeline. The current one pulls tweets based on [MailChimp](http://mailchimp.com) filters. See below:

`val terms = List("MailChimp", "Mailchimp", "MailChimp Status", "Mailchimp Status", "MailChimp UX", "Mailchimp UX", "MailChimp Design","Mailchimp Design", "MailChimp API", "Mailchimp API", "Mandrill", "mandrillapp", "TinyLetter", "Tinyletter")
hosebirdEndpoint.trackTerms(terms)`

* Publish messages to Kafka Topic through command-line **Kafka Producer**
  *  `bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topic_name`
* Pull messages from Kafka Consumer into Akka ActorPublisher
* Push Messages through an Akka Stream/Runnable Flow and undergo transformation (Source)
* Subscriber reads the messages from the Akka Stream/Runnable Flow (Sink)
* Subscriber/Sink dumps the transformed to the console

**Note**: Make sure the kakfa topics (yes Twitter Pipeline has 2) in `SimpleActorPipeline.scala` match the one you created during **Getting Started**

