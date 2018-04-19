import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import mySystem._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._

object mySystem {
  lazy val ourSystem = ActorSystem("ourExampleSystem")
}

class Pongy extends Actor {
  val log = Logging(context.system, this)

  override def receive : PartialFunction[Any, Unit] = {
    case "ping" =>
      log.info("Got a ping -- ponging back!")
      sender ! "pong"
      context.stop(self)
  }

  override def postStop() : Unit = log.info("pongy going down")
}

class Pingy extends Actor {
  val log = Logging(context.system, this)

  override def receive : PartialFunction[Any, Unit] = {
    case pongyRef: ActorRef =>
      implicit val timeout = Timeout(2 seconds)
      val future = pongyRef ? "ping"
      pipe(future) to sender
  }

  override def postStop() = log.info("pingy going down")
}

class Master extends Actor {
  val log = Logging(context.system, this)
  val pingy = ourSystem.actorOf(Props[Pingy], name = "pingy")
  val pongy = ourSystem.actorOf(Props[Pongy], name = "pongy")

  override def receive : PartialFunction[Any, Unit] = {
    case "start" =>
      pingy ! pongy
    case "pong" =>
      log.info("got a pong back")
      context.stop(self)
  }

  override def postStop() : Unit = log.info("master going down")
}

object CommunicatingAsk extends App {
  val masta = ourSystem.actorOf(Props[Master], name = "masta")

  masta ! "start"
  Thread.sleep(1000)
  ourSystem.terminate()
}