import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

class LifeCycleActor extends Actor {
  val log = Logging(context.system, this)
  // 이 구문이 var가 되어야하는 이유는? 초기화 할수 없는 값이기 때문에 val로 선언할 수 없다.
  // TODO : = _ 의 의미는? 이렇게 선언하는건 어떤의미 인가?
  // ActorRef 타입이면 참조값? 아니면 null로 초기화
  var child: ActorRef = _
  override def receive = {
    case num: Double => log.info(s"got a double - $num")
    case num: Int => log.info(s"got a int - $num")
    case lst: List[_] => log.info(s"list - ${lst.head}, ...")
    case txt: String => child ! txt
  }

  override def preStart(): Unit = {
    log.info(s"about to start : override def preStart()")
    child = context.actorOf(Props[StringPrinter], "kiddo")
  }

  override def preRestart(reason: Throwable, msg: Option[Any]): Unit = {
    log.info(s"about to restart because of $reason, during message $msg")
    super.preRestart(reason, msg)
  }

  override def postRestart(reason: Throwable): Unit = {
    log.info(s"just restarted due to $reason")
    super.postRestart(reason)
  }

  override def postStop() = log.info(s"just stopped")
}

class StringPrinter extends Actor {
  val log = Logging(context.system, this)
  override def receive = {
    case msg => log.info(s"child got message '$msg'")
  }
  override def preStart(): Unit = log.info(s"child about to start.")
  override def postStop(): Unit = log.info(s"child just stopped.")
}

object ActorsLifeCycle extends App {
  val ourSystem = ActorSystem("mysystem")
  val testy = ourSystem.actorOf(Props[LifeCycleActor], "testy")
  testy ! math.Pi
  Thread.sleep(1000)    // 동시성 프로그램인데 자꾸 sleep을 주나? 이해를 위해서라고 봐야하나? 순서 없이 동작해야 정상일텐데?
  testy ! 7
  Thread.sleep(1000)    // TODO : sleep 모두 죽이고 어떻게 되는지 확인해보자
  testy ! "hi there!"
  Thread.sleep(1000)
  testy ! List(1,2,3)
  Thread.sleep(1000)
  testy ! Nil
  Thread.sleep(1000)
  testy ! "sorry about that"
  Thread.sleep(1000)
  ourSystem.stop(testy)
  Thread.sleep(1000)
  ourSystem.terminate()

}
