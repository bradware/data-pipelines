import akka.actor.Props
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{WatermarkRequestStrategy, ActorSubscriber}
import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer}

/*
  ==============================
  Kafka ActorSubscriber below
  ==============================
*/

// Companion object
object TweetSubscriber {
  def props[K,V](producer: KafkaProducer[K, V], kafkaTopic: String): Props = Props(new TweetSubscriber(producer, kafkaTopic))
}

// ActorSubscriber that publishes to Kafka Topic for the Akka Stream
class TweetSubscriber[K,V](producer: KafkaProducer[K, V], kafkaTopic: String) extends ActorSubscriber {
  val requestStrategy = new WatermarkRequestStrategy(50) // high volume of 50, low of 25

  def receive = {
    case OnNext(v: V) => producer.send(new ProducerRecord(kafkaTopic, v))
    case OnError(err: Exception) =>
      println(err, "TweetSubscriber received Exception in stream")
      context.stop(self)
    case OnComplete =>
      println("TweetSubscriber stream completed!")
      context.stop(self)
    case _ =>
  }
}
