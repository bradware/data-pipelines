package Main

import java.util.Properties

import org.apache.kafka.clients.consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.collection.JavaConversions._

/**
  * Created by bware on 1/27/16.
  */
object Main extends App {
  val props = new Properties()
  props.put("bootstrap.servers", "172.16.21.150:9092")
  props.put("group.id", "test")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "1000")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(List("test"));
  while (true) {
    val records = consumer.poll(100);
    for (record <- records) {
      println(s"offset = ${record.offset}, key = ${record.key}, value = ${record.value}");
    }
  }
}
