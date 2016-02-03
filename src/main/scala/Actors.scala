import akka.actor.{Props, Actor}
import akka.stream.actor.ActorSubscriberMessage.OnNext
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
object TwitterPublisher {
  def props[K, V](consumer: KafkaConsumer[K, V]): Props = Props(new TwitterPublisher(consumer))
}

/* ActorPublisher for the Akka Stream */
class TwitterPublisher[K, V](consumer: KafkaConsumer[K, V]) extends ActorPublisher[V] {
  var queue: mutable.Queue[V] = mutable.Queue()
  val POLL_TIME = 100 // time in MS

  def receive: Actor.Receive = {
    case Request(count) => publishTweets(count)
    case Cancel => context.stop(self)
  }

  def publishTweets(count: Long) = {
    var onNextCount = 0
    while(onNextCount < count) {
      if (queue.isEmpty) {
        pollTweets()
      }
      if (queue.nonEmpty && onNextCount < count) {
        val record = queue.dequeue()
        OnNext(record)
        println(record)
        onNextCount += 1
      }
    }
  }

  def pollTweets() = {
    val records = consumer.poll(POLL_TIME) // Kafka-Consumer data collection
    for (record <- records) {
      queue.enqueue(record.value) // Add more tweets to queue
    }
    /*
    if (queue.nonEmpty) {
      println("New Backlog: " + queue) // Print out state of queue after new data is polled off kafka-consumer
    }
    */
  }
}

// Companion object
object TwitterSubscriber {
  def props(producer: KafkaProducer[String, Array[Byte]]): Props = Props(new TwitterSubscriber(producer))
}

/* ActorSubscriber for the Akka Stream */
class TwitterSubscriber(producer: KafkaProducer[String, Array[Byte]]) extends ActorSubscriber {
  //var queue: mutable.Queue[Array[Byte]] = mutable.Queue()
  //val MAX_SIZE = 100
  override val requestStrategy = new WatermarkRequestStrategy(100)

  def receive = {
    case OnNext(bytes: Array[Byte]) => producer.send(new ProducerRecord("twitter-mailchimp2", bytes))
    case Cancel => context.stop(self)
  }
}