import java.util.Properties

import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.collection.JavaConversions._

import akka.actor.{ ActorRef, ActorSystem, Props }

// PIPELINE DEMO OVERVIEW
// 1. Push messages to an Akka Actor from the Kafka Consumer
// 2. Messages go through a actor transition and undergo transformation
// 3. Final actor dumps output to console
// Actor 1 ----> Actor 2 -----> Actor 3 -----> Dumps to console

object ActorMain extends App {
  // Akka Steam actor setup and creation
  val system = ActorSystem("Email")
  val emailActor = system.actorOf(Props[EmailActor], "EmailActor")

  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("group.id", "test")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "1000")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(List("email-message")) //Kafka-Consumer reading from the topic new-test

  println("data-pipeline-demo starting...")
  while (true) {
    val records = consumer.poll(100) // Kafka-Consumer data collection
    for (record <- records) { //Kafka-Consumer data message
      emailActor.tell(EmailMessage(record.value), ActorRef.noSender)
    }
  }
}