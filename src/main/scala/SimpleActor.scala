import akka.actor.{Props, Actor}

/*
  ==============================
  Simple Actors defined below
  ==============================
*/

// Data Structure to hold the message data
case class SimpleMessage(message: String)

// Actor 1 in SimpleActorPipeline
class SimpleActor extends Actor {
  val processor = context.system.actorOf(Props[SimpleProcessor], "SimpleProcessor")

  def receive = {
    case msg: SimpleMessage => processor ! msg
  }
}

// Actor 2 in SimpleActorPipeline
class SimpleProcessor extends Actor {
  val printer = context.system.actorOf(Props[SimplePrinter], "SimplePrinter")

  def receive = {
    case SimpleMessage(msg) => printer ! SimpleMessage(processMessage(msg)) // Send the current greeting back to the sender
  }

  def processMessage(message: String) = {
    message.toUpperCase
  }
}

// Actor 3 in SimpleActorPipeline
class SimplePrinter extends Actor {
  def receive = {
    case SimpleMessage(message) => println(message)
  }
}