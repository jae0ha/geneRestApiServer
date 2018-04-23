import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString

// Client class companion object
// TODO : 동반 객체에 props를 만든 이유가 뭘까? companion object에 대한 보다 확실한 이해가 필요
object Client {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[Client], remote, replies)
}

// Tcp 통신하는 Actor 구현을 이해하자
class Client(remote: InetSocketAddress, listener: ActorRef) extends Actor {

  // TODO : class 밖의 import akka.io.{ IO, Tcp } 와의 차이점은? 왜 여기에 별도로 추가 import를 했나?
  import Tcp._
  // 아래의 import문은 주석처리해도 동작에 지장없다. Actor에 포함되어있으니까?
  //import context.system

  // TODO : IO object에 대한 이해
  // TODO : Tcp
  IO(akka.io.Tcp) ! Connect(remote)

  override def receive: PartialFunction[Any, Unit] = {
    // fuck @ 를 생략할 수 있다. 내부 로직에서 호출 하지 않으니까... 근데 헷깔리잖어
    //case fuck @ CommandFailed(_: Connect) =>
    // _ 거시기... 명명해도 돌아감
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context stop self
    // @ : 패턴매칭, 'c @ '가 생략되지 않은이유? c를 사용하니까!
    case c @ Connected(remote, local) =>
      // Client actor에게 Connected message 돌려줌
      listener ! c
      // 새로운 connection actor
      // 왜? connection actor가 sender() 가 되어야하는겨? server와의 connection actor 그냥 server actor라고 이해하면되네?
      val connection = sender()
      // connection actor를 활성화 하기 위해선 Register message를 보내야한다!
      connection ! Register(self)
      // become을 이용해 connected state로 변경
      // Connected 이전엔 connection을 행동으로 이후엔
      // become 안쪽의 message 처리로 receive 메소드의 state 변경
      context become {
        // connection actor에 write
        // sender에게 보낼거면 뭐하러 받지?
        // 실제로는 처리하든 되돌려주던 할 것 예제라서 일케 되어있음
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! "write failed"
        // listener에 data 중계
        case Received(data) =>
          listener ! data
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener ! "connection closed"
          // same with - context.stop(self)
          context stop self
      }
  }
}