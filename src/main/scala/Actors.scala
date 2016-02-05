import java.math.BigInteger
import java.util.Properties

import akka.actor.{Props, Actor}
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{WatermarkRequestStrategy, ActorSubscriber, ActorPublisher}
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer}
import scala.collection.JavaConversions._
import scala.collection.mutable

/*
  ==============================
  Akka Actor code below
  ==============================
*/
// Data Structure to hold the message data
case class EmailMessage(message: String)

// Actor 1
class EmailActor extends Actor {
  val processor = context.system.actorOf(Props[EmailProcessor], "EmailProcessor")

  def receive = {
    case msg: EmailMessage => processor ! msg
  }
}

// Actor 2
class EmailProcessor extends Actor {
  val printer = context.system.actorOf(Props[EmailPrinter], "EmailPrinter")

  def receive = {
    case EmailMessage(mail) => printer ! EmailMessage(processMessage(mail)) // Send the current greeting back to the sender
  }

  def processMessage(message: String) = {
    message.toUpperCase.reverse
  }
}

// Actor 3
class EmailPrinter extends Actor {
  def receive = {
    case EmailMessage(message) => println(message)
  }
}

/*
  ==============================
  Akka Streams Code below
  ==============================
*/

// Data Structure to hold the Tweet message
case class SimpleTweet(message: String)

// Data Structure to encapsulate information about transformed Tweet object from twitter-hbc-stream json api
case class Tweet(text: String, created_at: java.util.Date, user_name: String,
                 user_screen_name: String, user_location: String, user_followers_count: String) {
  val tweet = text
  val createdAt = created_at
  val userName = user_name
  val userHandle = user_screen_name
  val userLocation = user_location
  val followerCount = user_followers_count
}

// Companion object
object TweetPublisher {
  def props[K, V](consumer: KafkaConsumer[K, V]): Props = Props(new TweetPublisher(consumer))
}

/* ActorPublisher for the Akka Stream */
class TweetPublisher[K, V](consumer: KafkaConsumer[K, V]) extends ActorPublisher[V] {
  var queue: mutable.Queue[V] = mutable.Queue()
  val POLL_TIME = 100 // time in MS

  def receive = {
    case Request(cnt) =>
      println("received request from TweetSubscriber")
      publishTweets()
    case Cancel =>
      println("cancelled message received from TweetSubscriber -- STOPPING")
      context.stop(self)
    case _ =>
      println("received some other message")
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

// Companion object
object TweetSubscriber {
  def props(producer: KafkaProducer[String, Array[Byte]]): Props = Props(new TweetSubscriber(producer))
}

/* ActorSubscriber for the Akka Stream */
class TweetSubscriber(producer: KafkaProducer[String, Array[Byte]]) extends ActorSubscriber {
  val requestStrategy = new WatermarkRequestStrategy(50)

  def receive = {
    case OnNext(bytes: Array[Byte]) =>
      println(bytes, "Received bytes from Publisher and now Producing push to kakfa topic twitter-mailchimp2")
      producer.send(new ProducerRecord("twitter-mailchimp2", bytes))
    case OnError(err: Exception) =>
      println(err, "TweetSubscriber receieved Exception in  stream")
      context.stop(self)
    case OnComplete =>
      println("TweetSubscriber stream completed!")
      context.stop(self)
    case _ =>
  }
}


/*
// Companion object
object FibonacciPublisher {
  def props[K, V](consumer: KafkaConsumer[K, V]): Props = Props(new FibonacciPublisher(consumer))
}

class FibonacciPublisher[K, V](consumer: KafkaConsumer[K, V]) extends ActorPublisher[V]  {
  var queue: mutable.Queue[V] = mutable.Queue()
  val POLL_TIME = 200 // time in MS

  //consumer stuff

  def receive = {
    case Request(cnt) =>
      println("[FibonacciPublisher] Received Request ({}) from Subscriber", cnt)
      sendFibs()
    case Cancel =>
      println ("[FibonacciPublisher] Cancel Message Received -- Stopping")
      context.stop(self)
    case _ =>
  }

  def sendFibs() = {
    while (isActive && totalDemand > 0) {
      if(queue.isEmpty) {
        pollNums()
      }
      if (queue.nonEmpty) {
        onNext(queue.dequeue())
      }
    }
  }

  def pollNums() = {
    val records = consumer.poll(POLL_TIME) // Kafka-Consumer data collection
    for (record <- records) {
      queue.enqueue(record.value) // Add more tweets to queue
      println(s"enqueued ${record.value}")
    }
  }
}

// Companion object
object SimpleTweetPublisher {
  def props(consumer: KafkaConsumer[String, String]): Props = Props(new TweetPublisher(consumer))
}

/* ActorPublisher for the Akka Stream */
class SimpleTweetPublisher(consumer: KafkaConsumer[String, String]) extends ActorPublisher[String] {
  var queue: mutable.Queue[String] = mutable.Queue()
  val POLL_TIME = 100 // time in MS

  def receive = {
    case Request(cnt) =>
      println("received request from TweetSubscriber")
      publishTweets(cnt)
    case Cancel =>
      println("cancelled message received from TweetSubscriber -- STOPPING")
      context.stop(self)
    case _ =>
      println("received some other message")
  }

  def publishTweets(count: Long) = {
    println("Total Demand issued by ActorSubscriber: " + totalDemand)
    while (isActive && totalDemand > 0) {
      if (queue.isEmpty) {
        pollTweets()
      }
      if (queue.nonEmpty) {
        val record = queue.dequeue()
        OnNext(record) // Push the new data to the ActorSubscriber
        println(s"publishTweets: ${record}")
      }
    }
    println("Exiting publishTweets...")
  }

  def pollTweets() = {
    val records = consumer.poll(POLL_TIME) // Kafka-Consumer data collection
    for (record <- records) {
      queue.enqueue(record.value) // Add more tweets to queue
    }
  }
}
*/
