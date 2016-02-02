import java.text.SimpleDateFormat
import java.util.{Locale, Properties, Date}
import java.util.concurrent.{LinkedBlockingQueue, BlockingQueue}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Sink, Source}
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint
import com.twitter.hbc.core.event.Event
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.core.{Constants, HttpHosts}
import com.twitter.hbc.httpclient.auth.{OAuth1, Authentication}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer}
import scala.collection.JavaConversions._
import org.json4s._
import org.json4s.native.JsonMethods._

// FIX COMMENTS BELOW

/*
  =============================
  PIPELINE DEMO OVERVIEW
    1. Pull messages from Kafka Consumer into Akka ActorPublisher
    2. Push Messages through an Akka Stream/Runnable Flow and undergo some transformation (Source)
    3. Subscriber needs to read the messages from the Akka Stream/Runnable Flow (Sink)
    4. Subscriber/Sink dumps the transformed to the console
  =============================

  =============================
  IMPLEMENTATION
    FLOW: ActorPublisher(Source) ---> Stream ---> ActorSubscriber(Sink)
    FLOW: ActorPublisher(Source) ---> Stream ---> Sink.actorRef(Sink)  ***What I'm using
    FLOW: Source.actorRef(Source) ---> Stream ---> Sink.actorRef(Sink) // Built in simple source and sink
    FLOW: Source.actorRef(Source) ---> Stream ---> ActorSubscriber(Sink)
  =============================
*/
object TwitterMain extends App {
  // Twitter authentication credentials
  val CONSUMER_KEY = "Fn2GkcTo7MTXBUTH86gCcTCIg"
  val CONSUMER_SECRET = "UHcvIxWHjQl7M3VOvqQTNnRL3YAAmdTlFw9XL40vWl3waoPkOf"
  val ACCESS_TOKEN = "4870020185-ebPfDGBbjSTBX6aSkV11u9uuqokjRG9rAAi7LEv"
  val SECRET_TOKEN = "nHyoABn6hO1PJc7JgAmc9IRd3m9vD8Kzsd3hQ7eaVIJ4S"

  // Akka Actor and Producer/Subscriber setup
  implicit val system = ActorSystem("Twitter-MailChimp")
  implicit val materializer = ActorMaterializer()

  // Setting up props for Kafka Producer
  val prodProps = new Properties();
  prodProps.put("bootstrap.servers", "localhost:9092")
  prodProps.put("acks", "all")
  prodProps.put("retries", "0")
  prodProps.put("batch.size", "16384")
  prodProps.put("linger.ms", "1")
  prodProps.put("buffer.memory", "33554432")
  prodProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  prodProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  // Setting up props for Kafka Consumer
  val consProps = new Properties()
  consProps.put("bootstrap.servers", "localhost:9092")
  consProps.put("group.id", "data-pipeline-demo-consumer")
  consProps.put("enable.auto.commit", "true")
  consProps.put("auto.commit.interval.ms", "1000")
  consProps.put("session.timeout.ms", "30000")
  consProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  consProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  // Instantiating Kafka Producer of raw twitter data
  val rawTwitterProducer = new KafkaProducer[String, String](prodProps)

  // Instantiating Kafka Consumer of raw twitter data
  val rawTwitterConsumer = new KafkaConsumer[String, String](consProps)
  rawTwitterConsumer.subscribe(List("twitter-mailchimp-raw")) //Kafka-Consumer listening from the topic

  // Instantiating Kafka Producer of transformed twitter data
  val twitterProducer = new KafkaProducer[String, Array[Byte]](prodProps)

  // Instantiating Kafka Consumer of transformed twitter data
  val twitterConsumer = new KafkaConsumer[String, String](consProps)
  twitterConsumer.subscribe(List("twitter-mailchimp")) //Kafka-Consumer listening from the topic

  /*
    ===============================
    Connecting to Twitter Stream
    ===============================
  */

  // Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream
  val msgQueue = new LinkedBlockingQueue[String](100000)
  val eventQueue = new LinkedBlockingQueue[Event](1000)

  // Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth)
  val hosebirdHosts = new HttpHosts(Constants.STREAM_HOST)
  val hosebirdEndpoint = new StatusesFilterEndpoint()

  // Filter out tweets by 'mailchimp'
  val terms = List("mailchimp")
  hosebirdEndpoint.trackTerms(terms)

  // Pass in Auth for HBC Stream
  val hosebirdAuth = new OAuth1(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, SECRET_TOKEN)

  // Setting up HBC client builder
  val clientBuilder = new ClientBuilder()
    .name("Hosebird-Client-Twitter-MailChimp")
    .hosts(hosebirdHosts)
    .authentication(hosebirdAuth)
    .endpoint(hosebirdEndpoint)
    .processor(new StringDelimitedProcessor(msgQueue))
    .eventMessageQueue(eventQueue)

  // Builds Twitter HBC
  val hosebirdClient = clientBuilder.build()
  // Attempts to establish a connection to Twitter HBC stream
  hosebirdClient.connect()

  // Read data from Twitter HBC
  val hbcTwitterStream = new Thread {
    override def run() = {
      while (!hosebirdClient.isDone()) {
        val tweet = msgQueue.take()
        // kafka producer publish tweet to kafka topic
        rawTwitterProducer.send(new ProducerRecord("twitter-mailchimp-raw", tweet))
      }
    }
  }
  println("connecting to twitter-hbc-api...")
  hbcTwitterStream.run()
  /*
   ===============================
   End of Connecting to Twitter
   ===============================
 */

  // Source in this example is an ActorPublisher
  val twitterPublisher = Source.actorPublisher[String](TwitterPublisher.props(rawTwitterConsumer))
  // ActorSubscriber is the sink that pushes back into the Kafka Producer
  val twitterSubscriber = Sink.actorSubscriber[Array[Byte]](TwitterSubscriber.props(twitterProducer))


  val runnableGraph = twitterPublisher
    .map(msg => parse(msg))
    .map(json => extractJSONFields(json))
    .map(tweet => serializeTweet(tweet))
    .to(twitterSubscriber)
  println("data-pipeline-demo starting...")
  runnableGraph.run()

}

// TODO
def serializeTweet(tweet: Tweet): Array[Byte] = {
  Array.empty
}

def extractJSONFields(json: JValue) = {
  // getting all of the fields for the TweetStruct
  val text = (json \ "text").extract[String]
  val created_at = formatTwitterDate((json \ "created_at").extract[String])
  val user_name = ((json \ "user") \ "name").extract[String]
  val user_screen_name = ((json \ "user") \ "screen_name").extract[String]
  val user_location = ((json \ "user") \ "location").extract[String]
  val user_followers_count = ((json \ "user") \ "followers_count").extract[String]

  // instantiating a TweetStruct
  val tweetStruct = new Tweet(text, created_at, user_name, user_screen_name, user_location, user_followers_count)
  tweetStruct
}

def formatTwitterDate(date: String) = {
  val TWITTER_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z"
  val sf = new SimpleDateFormat(TWITTER_FORMAT, Locale.ENGLISH)
  sf.setLenient(true)
  sf.parse(date)
}

