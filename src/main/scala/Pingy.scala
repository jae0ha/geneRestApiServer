import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSystem, Identify, Props}
import akka.event.Logging
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Pingy extends Actor {
  def receive: PartialFunction[Any, Unit] = {
    case pongyRef: ActorRef =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      val future = pongyRef ? "ping"
      pipe(future) to sender
  }
}

class Runner extends Actor {
  val log = Logging(context.system, this)
  val pingy = context.actorOf(Props[Pingy], "pingy")
  override def receive: PartialFunction[Any, Unit] = {
    case "start" =>
      val path = context.actorSelection("akka.tcp://PongyDimention@192.168.1.35:24321/user/pong")
      path ! Identify(0)
    case ActorIdentity(0, Some(ref)) =>
      pingy ! ref
    case ActorIdentity(0, None) =>
      log.info("Something's wrong -- no pongy anywhere!")
      context.stop(self)
    case "pong" =>
      log.info("got a pong from anthor dimension.")
      context.stop(self)
  }
}

object RemotingPingySystem extends App {

  val REMOTE_PORT = 24567
  val SLEEP_TIME = 20000

  def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
       |akka {
       |  actor.provider = "akka.remote.RemoteActorRefProvider"
       |  remote {
       |    enabled-transports = ["akka.remote.netty.tcp"]
       |    netty.tcp {
       |      hostname = "192.168.1.148" // 자신 고유 주소 (보낼곳의 주소와는 다르다)
       |      port = $port // 자신 고유 포트 (보낼곳의 포트완느 다르다)
       |    }
       |  }
       |}
     """)

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))

  val system: ActorSystem = remotingSystem(name = "PingyDimension", port = REMOTE_PORT)
  val runner = system.actorOf(Props[Runner], name = "runner")
  runner ! "start"
  Thread.sleep(SLEEP_TIME)
  system.terminate()
}