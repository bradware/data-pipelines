import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import com.twitter.chill.{KryoInstantiator, KryoPool}
import org.json4s._
import org.objenesis.strategy.SerializingInstantiatorStrategy

object Util {
  /*
    Serialize the Tweet object into a byte array
  */
  def serialize[T](t: T): Array[Byte] = {
    val kryoPool = KryoPool.withByteArrayOutputStream(10, new KryoInstantiator())
    kryoPool.toBytesWithClass(t)
  }

  /*
    Deserialize the Tweet object into a byte array
  */
  def deserialize[T](byteArray: Array[Byte]): T = {
    val kryoPool = KryoPool.withByteArrayOutputStream(10, new KryoInstantiator().setInstantiatorStrategy(new SerializingInstantiatorStrategy()))
    kryoPool.fromBytes(byteArray).asInstanceOf[T]
  }

  /*
    Constructing the RichTweet object from raw Tweet JSON
  */
  def extractTweetFields(json: JValue) = {
    implicit val formats = DefaultFormats
    val text = (json \ "text").extract[String]
    val created_at = formatTweetDate((json \ "created_at").extract[String])
    val user_name = ((json \ "user") \ "name").extract[String]
    val user_screen_name = ((json \ "user") \ "screen_name").extract[String]
    val user_location = ((json \ "user") \ "location").extract[String]
    val user_followers_count = ((json \ "user") \ "followers_count").extract[String]

    // instantiating a TweetStruct
    val richTweet = new RichTweet(text, created_at, user_name, user_screen_name, user_location, user_followers_count)
    richTweet
  }

  /*
   Helper method to format date of raw Twitter date
  */
  def formatTweetDate(date: String) = {
    val TWITTER_FORMAT = "EEE MMM dd HH:mm:ss ZZZZZ yyyy"
    val sf = new SimpleDateFormat(TWITTER_FORMAT, Locale.ENGLISH)
    sf.setLenient(true)
    var tweetDate = new Date()
    try {
      tweetDate = sf.parse(date)
    }
    catch {
      case e: Exception => println(e)
    }
    tweetDate
  }
}
