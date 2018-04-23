import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging
import com.typesafe.config.{Config, ConfigFactory}

// TODO : 테스트
class PPongy extends Actor {
  val log = Logging(context.system, this)
  override def receive: PartialFunction[Any, Unit] = {
    case "ping" =>
      log.info("Got a ping -- ponging back!!")
      sender ! "pong"
      context.stop(self)
  }

  override def postStop(): Unit = log.info("pongy going down")
}

object RemotingPongySystem extends App {

  val REMOTE_PORT = 24321
  val SLEEP_TIME = 30000
  def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
       akka {
         actor.provider = "akka.remote.RemoteActorRefProvider"
         remote {
           enabled-transports = ["akka.remote.netty.tcp"]
           netty.tcp {
             hostname = "192.168.1.35"  // 자신 고유 주소
             port = $port // 자신 고유 포트
           }
         }
       }
     """)

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))
  val system = remotingSystem(name = "PongyDimension", port = REMOTE_PORT)
  val pongy = system.actorOf(Props[PPongy], name = "pongy")
  Thread.sleep(SLEEP_TIME)
  system.terminate()
}