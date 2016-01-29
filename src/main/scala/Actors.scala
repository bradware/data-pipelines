import akka.actor.{ Props, Actor }
import akka.stream.actor.ActorPublisher
import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.collection.JavaConversions._

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

/* Actor for the Akka Stream */
class TwitterActor(consumer: KafkaConsumer[String, String]) extends ActorPublisher[String] {
  consumer.subscribe(List("twitter-mailchimp")) //Kafka-Consumer reading from the topic new-test

  /*
    Read through Actor Publisher docs to see what to implement
  */
}

object TwitterActor {
  def props(consumer: KafkaConsumer[String, String]): Props = Props(new TwitterActor(consumer))
}

