import java.util.Properties
import akka.actor.{ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.collection.JavaConversions._

/*
  =============================
  PIPELINE DEMO OVERVIEW
    1. Pull messages from Kafka Consumer into Akka ActorPublisher
    2. Push Messages through an Akka Stream/Runnable Flow and undergo some transformation (Source)
    3. Subscriber needs to read the messages from the Akka Stream/Runnable Flow (Sink)
    4. Subscriber/Sink dumps the transformed to the console
  =============================
  =============================
  OPTIONS FOR IMPLEMENTATION
    FLOW: ActorPublisher(Source) ---> Stream ---> ActorSubscriber(Sink)
    FLOW: ActorPublisher(Source) ---> Stream ---> Sink.actorRef(Sink)  *** IN USE BELOW
    FLOW: Source.actorRef(Source) ---> Stream ---> Sink.actorRef(Sink) // Built in simple source and sink
    FLOW: Source.actorRef(Source) ---> Stream ---> ActorSubscriber(Sink)
  =============================
*/
object StreamMain extends App {
  implicit val system = ActorSystem("SimpleTweets")
  implicit val materializer = ActorMaterializer()

  // Setting up props for Kafka Consumer
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("group.id", "simple-tweets-consumer")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "1000")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  // Instantiating Kafka Consumer
  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(List("simple-tweets")) //Kafka-Consumer listening from the topic

  // Source in this example is an ActorPublisher
  val twitterSource = Source.actorPublisher[String](TwitterPublisher.props(consumer))
  // Sink just prints to console, ActorSubscriber is not used
  val consoleSink = Sink.foreach[String](tweet => {
    println(tweet)
    Thread.sleep(2000) // simulate how akka-streams handles Backpressure
  })

  val stream = twitterSource
    // transform message to upper-case
    .map(msg => msg.toUpperCase)
    // connecting to the sink
    .to(consoleSink)
  println("simple-tweets data-pipeline starting...")
  stream.run()
}
