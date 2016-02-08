import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import org.apache.kafka.clients.consumer.KafkaConsumer

import scala.collection.JavaConversions._
import scala.collection.mutable

/*
  Companion object
*/
object TweetPublisher {
  def props[K, V](consumer: KafkaConsumer[K, V]): Props = Props(new TweetPublisher(consumer))
}

/*
  ActorPublisher that pulls from Kafka Topic and and sends to ActorSubscriber
*/
class TweetPublisher[K, V](consumer: KafkaConsumer[K, V]) extends ActorPublisher[V] {
  var queue: mutable.Queue[V] = mutable.Queue()
  val POLL_TIME = 100 // time in MS

  def receive = {
    case Request(cnt) => publishTweets()
    case Cancel =>
      println("Cancel message received in TweetPublisher -- STOPPING")
      context.stop(self)
    case _ => println("Received some other message in TweetPublisher")
  }

  def publishTweets() = {
    while (isActive && totalDemand > 0) {
      if(queue.isEmpty) {
        pollTweets()
      }
      if (queue.nonEmpty) {
        onNext(queue.dequeue())
      }
    }
  }

  def pollTweets() = {
    val records = consumer.poll(POLL_TIME) // Kafka-Consumer data collection
    for (record <- records) {
      queue.enqueue(record.value) // Add more tweets to queue
    }
  }
}
