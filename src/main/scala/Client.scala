import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp
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
  // 아래의 import context.system 를 생략하면 compile error가 발생한다.
  // could not find implicit value for parameter system: akka.actor.ActorSystem / IO(akka.io.Tcp) ! Connect(remote)
  // 왜 그럴까?
  // akka.io.IO의 apply method 내용!!!!!!
  // def apply[T <: Extension](key: ExtensionId[T])(implicit system: ActorSystem): ActorRef = key(system).manager
  // (implicit system: ActorSystem) 이거!!! 빌어먹을!! 빼면 컴파일 에러나는 부분을 생략하면 어쩌라는 거냐???
  // 앞으로 이런경우가 발생하면 빌어먹을 object의 apply method가 어떻게 되먹었는지 확인하자!
  // 그런데 import context.system 으로 해결되는 이유는 뭐셔? 암묵적으로 처박히는겨? 걍 명시적으로 하지?
  // import context.system  // 원본

  //IO(akka.io.Tcp) ! Connect(remote)
  // 이렇게 바꾸면 되나 해봤다... 된다...
  //val fuckImplicitSystem = context.system // fuck 본
  //IO(akka.io.Tcp)(fuckImplicitSystem) ! Connect(remote)

  // 또 바꿔보자 이것도 컴파일 에러는 안난다.
  // IO(akka.io.Tcp)(context.system) ! Connect(remote)
  // TODO : 위의 fucking한 상황을 동작에서도 문제가 없는지 확인하자... 문제 없어 보이지만...

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