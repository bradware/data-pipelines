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

// Abstract Tweet
trait Tweet {
  val text: String
}
// Simple Tweet that just hold's the text
case class SimpleTweet(text: String) extends Tweet {
  override def toString = {
    s"Text: ${text}"
  }
}
// RichTweet to encapsulate information about transformed Tweet object from twitter-hbc-stream json api
case class RichTweet(text: String, createdAt: java.util.Date, userName: String,
                 userHandle: String, userLocation: String, userFollowerCount: String) extends Tweet {

  override def toString = {
   s"""Text: ${text}
        |CreatedAt: ${createdAt}
        |UserName:  ${userName}
        |UserHandle: ${userHandle}
        |UserLocation: ${userLocation}
        |UserFollowerCount: ${userFollowerCount}""".stripMargin
  }

  override def equals(o: Any) = o match {
    case otherTweet: RichTweet => this.text == otherTweet.text && this.createdAt == otherTweet.createdAt && this.userName == otherTweet.userName && this.userHandle == otherTweet.userHandle &&
                              this.userLocation == otherTweet.userLocation && this.userFollowerCount == otherTweet.userFollowerCount
    case _ => false
  }
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
    case Request(cnt) => publishTweets()
    case Cancel =>
      println("Cancelled message received in TweetPublisher -- STOPPING")
      context.stop(self)
    case _ => println("Received some other message in TwetPublisher")
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
  def props[K,V](producer: KafkaProducer[K, V]): Props = Props(new TweetSubscriber(producer))
}
/* ActorSubscriber for the Akka Stream */
class TweetSubscriber[K,V](producer: KafkaProducer[K, V]) extends ActorSubscriber {
  val requestStrategy = new WatermarkRequestStrategy(50)

  def receive = {
    case OnNext(v: V) => producer.send(new ProducerRecord("twitter-mailchimp", v))
    case OnError(err: Exception) =>
      println(err, "TweetSubscriber received Exception in stream")
      context.stop(self)
    case OnComplete =>
      println("TweetSubscriber stream completed!")
      context.stop(self)
    case _ =>
  }
}
