import java.util.Properties

import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.collection.JavaConversions._

import akka.actor.{ ActorRef, ActorSystem, Props }

// PIPELINE DEMO OVERVIEW
// 1. Push messages to an Akka Stream from the Publisher
// 2. Messages go through a Runnable Flow in the Akka Stream and undergo some transformation
// 3. Subscriber needs to read the messages from the Akka Stream
// 4. Subscriber should dump the transformed data back out to console

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

  println("pipeline-demo starting...")
  while (true) {
    val records = consumer.poll(100) // Kafka-Consumer data Collection
    for (record <- records) { //Kafka-Consumer data message
      emailActor.tell(EmailMessage(record.value), ActorRef.noSender)
    }
  }
}