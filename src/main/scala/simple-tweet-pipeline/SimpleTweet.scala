/*
  Abstract Tweet
*/
trait Tweet {
  val text: String
}

/*
  Simple Tweet that just holds the text
*/
case class SimpleTweet(text: String) extends Tweet {
  override def toString = {
    s"Text: ${text}"
  }
}