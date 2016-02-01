import akka.actor.FSM.Failure
import akka.actor.{Props, Actor}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import org.apache.kafka.clients.consumer.KafkaConsumer
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

// Data Structure to hold the Tweet
case class Tweet(message: String)

// Companion object
object TwitterPublisher {
  def props(consumer: KafkaConsumer[String, String], MAX_BUFFER_SIZE: Int): Props = Props(new TwitterPublisher(consumer, MAX_BUFFER_SIZE))
}

/* ActorPublisher for the Akka Stream */
class TwitterPublisher(consumer: KafkaConsumer[String, String], MAX_BUFFER_SIZE: Int) extends ActorPublisher[String] {
  var queue: mutable.Queue[String] = mutable.Queue()
  val POLL_TIME = 100 //time to poll in MS

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
        onNext(queue.dequeue())
        onNextCount += 1
      }
    }
  }

  def pollTweets() = {
    if (queue.length < MAX_BUFFER_SIZE ) {
      val records = consumer.poll(POLL_TIME) // Kafka-Consumer data collection
      for (record <- records) {
        queue.enqueue(record.value) // Add more tweets to queue
      }
      if (queue.nonEmpty) {
        println("New Backlog: " + queue) // Print out state of queue after new data is polled off kafka-consumer
      }
    }
  }
}