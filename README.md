# Data Pipelines
* [Simple Actor Pipeline](//git.rsglab.com/bware/data-pipelines/blob/master/src/main/scala/simple-actor-pipeline/SimpleActorPipeline.scala)
* [Simple Tweet Pipeline](//git.rsglab.com/bware/data-pipelines/blob/master/src/main/scala/simple-tweet-pipeline/SimpleTweetPipeline.scala)
* [Twitter Pipeline](//git.rsglab.com/bware/data-pipelines/blob/master/src/main/scala/twitter-pipeline/TwitterPipeline.scala)

## Technologies
* [Scala](//www.scala-lang.org/download)
* [Kafka](//kafka.apache.org/downloads.html) (v9)
* [Akka Streams](//doc.akka.io/docs/akka-stream-and-http-experimental/2.0.2/scala.html)
* [Twitter HBC API](//github.com/twitter/hbc)
* [Kryo - Twitter Chill](//github.com/twitter/chill)

## Getting Started
* download [Kafka 9](//kafka.apache.org/downloads.html)
* navigate to where you downloaded kafka 9: 
  * `cd kafka_2.11-0.9.0.0`
* `bin/zookeeper-server-start.sh config/zookeeper.properties`
* `bin/kafka-server-start.sh config/server.properties`
* Create raw data Kafka Topic (insert real name for topic_name): 
  * `bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name`
* Create transformed data Kafka Topic (insert real name for topic_name): 
  * `bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name`
* See new topics created:
  * `bin/kafka-topics.sh --list --zookeeper localhost:2181`

## Simple Actor Pipeline
### Overview
* Publish messages to Kafka Topic through command-line Kafka Producer
  *  `bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topic_name`
* Push messages to an Akka Actor from the Kafka Consumer (which polls from Kafka Topic)
* Messages go through an actor pipeline and undergo transformation
* Final actor dumps output to console

`KafkaTopic --> SimpleActor --> SimpleProcessor --> SimplePrinter --> Dumps to console`

**Note**: Make sure the kakfa topic in `SimpleActorPipeline.scala` matches the one you created during **Getting Started** and for the command line Kafka Producer

Run through `SimpleActorPipeline.scala`

## Simple Tweet Pipeline
### Overview
* Publish lowercase messages to Kafka Topic through command-line Kafka Producer
  *  `bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topic_name`
* Pull lowercase messages from Kafka Topic through the Kafka Consumer into Akka ActorPublisher
* Push lowercase messages through the Akka Stream which capitalizes and transforms to Simple Tweet objects
* Console Sinks reads Simple Tweets from the Akka Stream
* Console Sink dumps the Simple Tweet messages to the console

`KafkaTopic --> ActorPub --> Stream/Flow --> ConsoleSink`

**Note**: Make sure the kakfa topic in `SimpleTweetPipeline.scala` matches the one you created during **Getting Started** and for the command line Kafka Producer

Run through `SimpleTweetPipeline.scala`

## Twitter Pipeline
In `Config.scala` update the filter terms list and your auth credentials (see below) to correctly setup your Twitter HBC stream. The current one pulls tweets based on filtering for [MailChimp](//mailchimp.com).

```scala
val terms = List("MailChimp", "Mailchimp", "MailChimp Status", "Mailchimp Status", "MailChimp UX", "Mailchimp UX", "MailChimp Design","Mailchimp Design", "MailChimp API", "Mailchimp API", "Mandrill", "mandrillapp", "TinyLetter", "Tinyletter")
```

```scala
val CONSUMER_KEY = "insert_consumer_key"
val CONSUMER_SECRET = "insert_consumer_secret"
val ACCESS_TOKEN = "insert_access_token"
val SECRET_TOKEN = "insert_secret_token"
```

### Overview
* Pull raw json tweets from Twitter HBC client
* Push raw json tweets into Kafka topic through Kafka Producer
* Pull raw json tweets from Kafka Consumer and store in Akka Publisher
* Akka Publisher sends raw json through first stream to transform/serialize to `Tweet` object
* Akka Subscriber takes serialized `Tweet` object and uses Kafka Producer to push to another Kafka Topic
* Kafka Consumer inside Akka Publisher pulls from topic and sends the serialized `Tweet` through final stream
* Final Stream deserializes the `Tweet` object and dumps to console sink

`TwitterHBC --> KafkaProd --> KafkaTopic --> ActorPub --> RawStream --> ActorSub --> KafkaTopic --> ActorPub -->       TransformedStream --> ConsoleSink`

**Note**: Make sure the kakfa topics (yes Twitter Pipeline has 2) in `TwitterPipeline.scala` match the one you created during **Getting Started** 

Run through `TwitterPipeline.scala`

