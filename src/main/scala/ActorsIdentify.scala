import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging

class CheckActor extends Actor {

  import akka.actor.{Identify, ActorIdentity}

  val log = Logging(context.system, this)

  def receive: PartialFunction[Any, Unit] = {
    case path: String =>
      log.info(s"checking path => $path")
      // 액터 경로에 있는 액터 참조 얻기
      context.actorSelection(path) ! Identify(path)
    // 어떤 아카 액터가 Identify 메세지를 받으면, 자동으로 자신의 ActorRef가 들어있는 ActorIdentify 메세지로 응답한다.
    case ActorIdentity(path, Some(ref)) =>
      log.info(s"found actor $ref on $path")
      // 액터 선택이 가르키는 액터가 없다면, 아무 ActorRef 객체가 없는 ActorIdentify 메세지를 송신한 액터에 돌려준다.
      // 송신한 액터는 case ActorIdentify(path, None)으로 받게 된다.
    case ActorIdentity(path, None) =>
      log.info(s"could not find an actor on $path")
  }
}

object ActorsIdentify extends App {

  val ourSystem = ActorSystem("mysystem")

  val checker = ourSystem.actorOf(Props[CheckActor], "checker")
  // ../* 현재 액터와 모든 형제들을 의미한다.
  checker ! "../*"
  Thread.sleep(1000)
  checker ! "../../*"
  Thread.sleep(1000)
  checker ! "/system/*"
  Thread.sleep(1000)
  checker ! "/user/cheker2"
  Thread.sleep(1000)
  checker ! "akka://OurExampleSystem/system"
  Thread.sleep(1000)
  ourSystem.stop(checker)
  Thread.sleep(1000)
  ourSystem.terminate()

  // TODO Option 알아볼것
  /*ref: Option[ActorRef]*/
  // TODO 문법해석 해서 주석달것
  /*@inline final def orNull[A1 >: A](implicit ev: Null <:< A1): A1 = this getOrElse ev(null)*/
}
