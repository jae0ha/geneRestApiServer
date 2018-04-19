import akka.actor.{Actor, ActorSystem, Props}
import yourSystem._

object yourSystem {
  lazy val ourSystem = ActorSystem("ourExampleSystem")
}

/*class StringPrinter extends Actor {
  val log = Logging(context.system, this)

  override def receive : PartialFunction[Any, Unit] = {
    case msg => log.info(s"child got message '$msg'")
  }

  override def preStart(): Unit = log.info(s"child about to start.")
  override def postStop(): Unit = log.info(s"child just stopped.")
}*/

// forward!!!
class Router extends Actor {
  var i = 0
  val children = for (_ <- 0 until 4) yield context.actorOf(Props[StringPrinter])
  override def receive : PartialFunction[Any, Unit] = {
    case "stop" => context.stop(self)
    case msg =>
      children(i) forward msg
      i = (i + 1) % 4
  }
}

object CommunicatingRouter extends App {
  val router = ourSystem.actorOf(Props[Router], name = "router")
  router ! "HI."
  router ! "I'm talking to you!"
  router ! "this time is c chiild!"
  router ! "d it's your time"
  router ! "a again!"
  Thread.sleep(1000)
  router ! "stop"
  Thread.sleep(1000)
  ourSystem.terminate()
}