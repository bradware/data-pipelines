import java.util.Properties
import akka.actor.{ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.collection.JavaConversions._

// PIPELINE DEMO OVERVIEW
// 1. Pull messages from Kafka Consumer into Akka ActorPublisher
// 2. Push Messages through an Akka Stream/Runnable Flow and undergo some transformation (Source)
// 3. Subscriber needs to read the messages from the Akka Stream/Runnable Flow (Sink)
// 4. Subscriber/Sink dumps the transformed to the console

// WAYS TO IMPLEMENT THIS
// FLOW: ActorPublisher(Source) ---> Stream ---> ActorSubscriber(Sink)
// FLOW: ActorPublisher(Source) ---> Stream ---> Sink.actorRef(Sink)  ***What I'm using
// FLOW: Source.actorRef(Source) ---> Stream ---> Sink.actorRef(Sink) // Built in simple source and sink
// FLOW: Source.actorRef(Source) ---> Stream ---> ActorSubscriber(Sink)
object StreamMain extends App {
  implicit val system = ActorSystem("Twitter-MailChimp")
  implicit val materializer = ActorMaterializer()

  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("group.id", "data-pipeline-demo-consumer")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "1000")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(List("tweets")) //Kafka-Consumer listening from the topic

  // Source in this example is an ActorPublisher
  val twitterSource = Source.actorPublisher[String](TwitterPublisher.props(consumer, 200))
  // Sink just prints to console, ActorSubscriber is not used
  val consoleSink = Sink.foreach[String](tweet => {
    println(tweet)
    Thread.sleep(2000)
  })

  val runnableGraph = twitterSource
    // transform message to upper-case
    .map(msg => msg.toUpperCase)
    // transform message to reverse value
    // .map(msg => msg.reverse)
    // connecting to the sink
    .to(consoleSink)

  println("data-pipeline-demo starting...")
  runnableGraph.run()
}
