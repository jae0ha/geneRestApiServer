import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

class SecondCheckActor extends Actor {
  import akka.actor.{ Identify, ActorIdentity }
  val log = Logging(context.system, this)

  override def receive: PartialFunction[Any, Unit] = {
    // pattern 매칭시 같은 패턴이 case문에 있을 경우 앞쪽이 우선시 된다 당연하지?
    /*case path: String =>
      log.info(s"how to handle this case class pattern??? ang? $path")*/
    case path: String =>
      log.info(s"checking path => $path")
      context.actorSelection(path) ! Identify(path)
    case ActorIdentity(path, Some(ref)) =>
      //log.info(s"found actor $ref on $path")
      ref match {
        case haha: ActorRef => log.info(s"found actorRef '$haha' on $path")
        case _ => log.info(s"found something '$ref' on $path")
      }
    case ActorIdentity(path, None) =>
      log.info(s"could not find an actor on $path")
  }
}

object ActorsIdentifyFuck extends App {
  val fuckSystem = ActorSystem("fuckSystem")
  val SLEEP_TIME = 1000

  val checker = fuckSystem.actorOf(Props[SecondCheckActor], "checker")
  checker ! "../*"
  Thread.sleep(SLEEP_TIME)
  checker ! "../../*"
  Thread.sleep(SLEEP_TIME)
  checker ! "/system/*"
  Thread.sleep(SLEEP_TIME)
  checker ! "/usr/checker2"
  Thread.sleep(SLEEP_TIME)
  checker ! "akka://OurExampleSystem/system"
  Thread.sleep(SLEEP_TIME)
  checker ! "/"
  Thread.sleep(SLEEP_TIME)
  fuckSystem.stop(checker)
  fuckSystem.terminate()
}
