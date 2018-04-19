import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorKilledException, Kill, OneForOneStrategy, Props}
import akka.event.Logging
import mySystem._

class Naughty extends Actor {
  val log = Logging(context.system, this)

  override def receive : PartialFunction[Any, Unit] = {
    case s: String => log.info(s)
    case msg => throw new RuntimeException
  }

  override def postRestart(t: Throwable) : Unit = log.info("naughty restarted")
}

class Supervisor extends Actor {
  val child = context.actorOf(Props[Naughty], "victim")
  // TODO : PartialFunction.empty 의 용법을 알아보자
  override def receive = PartialFunction.empty
  override val supervisorStrategy =
    OneForOneStrategy() {
      case ake: ActorKilledException => Restart
      case _ => Escalate
    }
}

object SupervisionKill extends App {
  val s = ourSystem.actorOf(Props[Supervisor], name = "super")
  ourSystem.actorSelection( path = "/user/super/*") ! Kill
  ourSystem.actorSelection( path = "/user/super/*") ! "sorry about that"
  ourSystem.actorSelection( path = "/user/super/*") ! 222
  Thread.sleep(1000)
  ourSystem.stop(s)
  Thread.sleep(1000)
  ourSystem.terminate()
}