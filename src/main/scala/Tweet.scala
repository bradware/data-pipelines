/*
  ==============================
  Tweet Models below
  ==============================
*/

// Abstract Tweet
trait Tweet {
  val text: String
}

// Simple Tweet that just holds the text
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