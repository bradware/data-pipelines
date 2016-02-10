import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection.JavaConversions._

/*
  =============================
  TWITTER PIPELINE OVERVIEW
    1. Pull raw json tweets from Twitter HBC client
    2. Push raw json tweets into Kafka topic through Kafka Producer
    3. Pull raw json tweets from Kafka Consumer and store in Akka Publisher
    4. Akka Publisher sends raw json through first stream to transform/serialize to Tweet object
    5. Akka Subscriber takes serialized Tweet object and uses Kafka Producer to push to another Kafka Topic
    6. Kafka Consumer inside Akka Publisher pulls from topic and sends the serialized tweet through final stream
    7. Final Stream deserializes the Tweet object and dumps to console sink
  =============================
*/
object TwitterPipeline extends App {
  implicit val system = ActorSystem("TwitterPipeline")
  implicit val materializer = ActorMaterializer()

  // Kafka Topics
  val RAW_TOPIC = "twitter-pipeline-raw"
  val TOPIC = "twitter-pipeline"

  // Props for Kafka Consumer and Producer
  val prodProps = Config.kafkaProducerPropSetup
  val consProps = Config.kafkaConsumerPropSetup

  // Raw twitter Kafka Producer and Consumer setup
  val rawTwitterProducer = new KafkaProducer[String, String](prodProps)
  val rawTwitterConsumer = new KafkaConsumer[String, String](consProps)
  rawTwitterConsumer.subscribe(List(RAW_TOPIC))

  // Transformed twitter Kafka Consumer and Producer setup
  // Changing prop value of serializer & deserializer to handle Array[Byte]
  prodProps.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
  val twitterProducer = new KafkaProducer[String, Array[Byte]](prodProps)
  consProps.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  val twitterConsumer = new KafkaConsumer[String, Array[Byte]](consProps)
  twitterConsumer.subscribe(List(TOPIC))

  // Setting up HBC client builder
  val hosebirdClient = Config.twitterHBCSetup

  // Reads data from Twitter HBC on own thread
  val hbcTwitterStream = new Thread {
    override def run() = {
      hosebirdClient.connect() // Establish a connection to Twitter HBC stream
      while (!hosebirdClient.isDone()) {
        val event = Config.eventQueue.take()
        val tweet = Config.msgQueue.take()
        // kafka producer publish tweet to kafka topic
        rawTwitterProducer.send(new ProducerRecord(RAW_TOPIC, tweet))
      }
      hosebirdClient.stop() // Closes connection with Twitter HBC stream
    }
  }

  println("twitter data-pipeline starting...")
  hbcTwitterStream.start() // Starts the thread which invokes run()

  // Source in this example is an ActorPublisher publishing raw tweet json
  val rawTweetSource = Source.actorPublisher[String](TweetPublisher.props(rawTwitterConsumer))
  // ActorSubscriber is the sink that uses Kafka Producer to push back into Kafka Topic
  val richTweetSink = Sink.actorSubscriber[Array[Byte]](TweetSubscriber.props(twitterProducer, TOPIC))

  // Akka Stream/Flow: ActorPublisher ---> raw JSON ---> Tweet ---> Array[Byte] ---> ActorSubscriber
  val rawStream = Flow[String]
    .map(msg => parse(msg))
    .map(json => Util.extractTweetFields(json))
    .map(tweet => Util.serialize[Tweet](tweet))
    .to(richTweetSink)
    .runWith(rawTweetSource)

  // Source in this example  is an ActorPublisher publishing transformed tweet json
  val richTweetSource = Source.actorPublisher[Array[Byte]](TweetPublisher.props(twitterConsumer))
  // Sink is simply the console
  val consoleSink = Sink.foreach[Tweet](tweet => {
    println("=========================================================================")
    println(tweet)
  })

  // Akka Stream/Flow: ActorPublisher ---> Array[Byte] ---> Tweet ---> ConsoleSink
  val transformedStream = Flow[Array[Byte]]
    .map(bytes => Util.deserialize[Tweet](bytes))
    .to(consoleSink)
    .runWith(richTweetSource)
}