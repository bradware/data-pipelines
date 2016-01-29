import java.util.Properties

import akka.actor.{ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.KafkaConsumer

// PIPELINE DEMO OVERVIEW
// 1. Pull messages from Kafka Consumer into Akka ActorPublisher
// 2. Push Messages through an Akka Stream/Runnable Flow and undergo some transformation (Source)
// 3. Subscriber needs to read the messages from the Akka Stream/Runnable Flow (Sink)
// 4. Subscriber/Sink dumps the transformed to the console

object StreamMain extends App {
  implicit val system = ActorSystem("Twitter-MailChimp")
  implicit val materializer = ActorMaterializer()

  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("group.id", "test")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "1000")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  val consumer = new KafkaConsumer[String, String](props)

  val twitterSource = Source.actorPublisher[String](TwitterActor.props(consumer))
  val consoleSink = Sink.foreach[String](println)

  val runnableGraph = twitterSource
    // transform message to upper-case
    .map(msg => msg.toUpperCase)
    // transform message to reverse value
    .map(msg => msg.reverse)
    // connecting to the sink
    .to(consoleSink)

  println("pipeline-demo starting...")
  runnableGraph.run()
}
