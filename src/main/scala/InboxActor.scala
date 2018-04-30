import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, Props}

import scala.concurrent.duration._

// TODO : 이런 방식의 사용도 있나?
// 단순히 패턴 매칭을 위한 선언으로 쓴듯
// 아무튼 불변하는 값을 박아두나 보다.
case object Greet

case object Fock

case class WhoToGreet(who: String)

case class Greeting(message: String)

case class Fucking(message: String)

/**
  * Inbox : actor간의 통신방식의 하나로 임의의 actor를 하나 만들어서 이 actor가 대신 보내고 받는다.
  *   - 동기적 회신을 받는다.
  *   - 따라서 타임아웃 시간동안은 멈춘다!
  *   - 고로 절대로 receive 핸들러 안에서 사용하면 안된다! 절대로!
  *   - 이런 특성때문에 주로 watch 메소드를 이용하여 다른 actor들을 감시하는데 사용한다.
  */
class Greeter extends Actor {
  var greeting = ""
  val fuck = "fuck!!!!"

  override def receive: PartialFunction[Any, Unit] = {
    case WhoToGreet(who) => greeting = s"hello, $who"
    case Greet => sender ! Greeting(greeting) // Send the current greeting back to the sender
    case Fock => sender ! Greeting(s"$greeting eat this $fuck")
  }
}

object HellowAkkaScala extends App {
  val system = ActorSystem("helloakka")
  val greeter = system.actorOf(Props[Greeter], "greeter")

  // val로 선언하는군 inbox는 actor 취급인가?
  // 설명을 보면 actor like란다...
  // 암튼 inbox 생성
  val inbox: Inbox = Inbox.create(system)

  // tell : send and forget 비동기
  greeter.tell(WhoToGreet("akka"), ActorRef.noSender)

  // inbox로 send
  inbox.send(target = greeter, msg = Greet)
  val Greeting(message1): Any = inbox.receive(5.seconds)
  println(s"Greeting: $message1")

  inbox.send(target = greeter, msg = Fock)
  val Greeting(fuck1): Any = inbox.receive(5.seconds)
  println(s"Fucking: $fuck1")

  val greetPrinter = system.actorOf(Props[GreetPrinter])
  /**
    * Schedules a message to be sent repeatedly with an initial delay and
    * frequency. E.g. if you would like a message to be sent immediately and
    * thereafter every 500ms you would set delay=Duration.Zero and
    * interval=Duration(500, TimeUnit.MILLISECONDS)
    *
    * Java & Scala API
    */

  system.scheduler.schedule(
    0.seconds, 1.seconds, receiver = greeter, message = Greet)(
    executor = system.dispatcher, sender = greetPrinter)
  system.scheduler.schedule(
    0.seconds, 1.seconds, receiver = greeter, message = Fock)(
    executor = system.dispatcher, sender = greetPrinter)

  // schedule 구현부분
  // currying 함수이용 http://wiki.ucsit.co.kr/dokuwiki/doku.php?id=currying
  //final def schedule(
  //                    initialDelay: FiniteDuration,
  //                    interval:     FiniteDuration,
  //                    receiver:     ActorRef,
  //                    message:      Any)(implicit
  //                                       executor: ExecutionContext,
  //                                       sender: ActorRef = Actor.noSender): Cancellable =
  //  schedule(initialDelay, interval, new Runnable {
  //    def run = {
  //      receiver ! message
  //      if (receiver.isTerminated)
  //        throw new SchedulerException("timer active for terminated actor")
  //    }
  //  })
}

class GreetPrinter extends Actor {
  override def receive: PartialFunction[Any, Unit] = {
    case Greeting(message) => println(message)
    case Fucking(message) => println(message)
  }
}

