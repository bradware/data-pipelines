import java.util.Properties

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.apache.kafka.clients.consumer.KafkaConsumer

import scala.collection.JavaConversions._

/*
  =============================
  SIMPLE TWEET PIPELINE OVERVIEW
    1. Use the Kafka Producer through the command line to send lowercase messages to the Kafka Topic
    2. Pull lowercase messages from Kafka Topic through the Kafka Consumer into Akka ActorPublisher
    3. Push lowercase messages through the Akka Stream which capitalizes and transforms to Simple Tweet objects
    4. Console Sinks reads Simple Tweets from the Akka Stream
    5. Console Sink dumps the Simple Tweet messages to the console

  KafkaTopic --> ActorPub --> Stream/Flow --> ConsoleSink
  =============================
  =============================
  OPTIONS FOR IMPLEMENTATION
    FLOW: ActorPublisher(Source) ---> Stream ---> ActorSubscriber(Sink)
    FLOW: ActorPublisher(Source) ---> Stream ---> Sink.actorRef(Sink)  *** IN USE BELOW
    FLOW: Source.actorRef(Source) ---> Stream ---> Sink.actorRef(Sink) // Built in simple source and sink
    FLOW: Source.actorRef(Source) ---> Stream ---> ActorSubscriber(Sink)
  =============================
*/
object SimpleTweetPipeline extends App {
  implicit val system = ActorSystem("SimpleTweetPipeline")
  implicit val materializer = ActorMaterializer()

  // Setting up props for Kafka Consumer
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("group.id", "simple-tweet-consumer")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "1000")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(List("simple-tweet-pipeline")) // Kafka-Consumer listening from the topic

  // Source in this example is an ActorPublisher from twitter-pipeline project
  val simpleTweetSource = Source.actorPublisher[String](TweetPublisher.props(consumer))
  // Sink just prints to console, ActorSubscriber is not used
  val consoleSink = Sink.foreach[Tweet](tweet => {
    println("CONSOLE SINK: " + tweet.text)
    Thread.sleep(1000) // simulate how akka-streams handles Backpressure
  })

  // starting the stream
  println("simple-tweet data-pipeline starting...")
  val stream = Flow[String]
    // transform text to upper-case
    .map(text => text.toUpperCase)
    // transforming text to SimpleTweet
    .map(text => new SimpleTweet(text))
    // connecting to the sink
    .to(consoleSink)
    .runWith(simpleTweetSource)
}
