import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, Props}
import scala.concurrent.duration._

// TODO : 이런 방식의 사용도 있나?
// 단순히 패턴 매칭을 위한 선언으로 쓴듯
// 아무튼 불변하는 값을 박아두나 보다.
case object Greet
case class WhoToGreet(who: String)
case class Greeting(message: String)

/**
  * Inbox : actor간의 통신방식의 하나로 임의의 actor를 하나 만들어서 이 actor가 대신 보내고 받는다.
  *   - 동기적 회신을 받는다.
  *   - 따라서 타임아웃 시간동안은 멈춘다!
  *   - 고로 절대로 receive 핸들러 안에서 사용하면 안된다! 절대로!
  *   - 이런 특성때문에 주로 watch 메소드를 이용하여 다른 actor들을 감시하는데 사용한다.
  */
class Greeter extends Actor {
  var greeting = ""

  override def receive: PartialFunction[Any, Unit] = {
    case WhoToGreet(who) => greeting = s"hello, $who"
    case Greet => sender ! Greeting(greeting) // Send the current greeting back to the sender
  }
}

object HellowAkkaScala extends App {
  val system = ActorSystem("helloakka")
  val greeter = system.actorOf(Props[Greeter], "greeter")

  // val로 선언하는군 inbox는 actor 취급인가?
  val inbox: Inbox = Inbox.create(system)

  // tell : send and forget 비동기
  greeter.tell(WhoToGreet("akka"), ActorRef.noSender)

  inbox.send(target = greeter, msg = Greet)
  val Greeting(message1): Any = inbox.receive(5.seconds)
  println(s"Greeting: $message1")

  val greetPrinter = system.actorOf(Props[GreetPrinter])
  system.scheduler.schedule(0.seconds, 1.seconds, greeter, Greet)(system.dispatcher, greetPrinter)
}

class GreetPrinter extends Actor {
  override def receive: PartialFunction[Any, Unit] = {
    case Greeting(message) => println(message)
  }
}

