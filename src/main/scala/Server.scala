import java.net.InetSocketAddress

import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}

class Server extends Actor {
  import Tcp._
  import context.system

  // Bind 명령을 메니저에 전달
  // port = 0 은 랜덤포트로 바인드됨 - 이렇게 쓸거면 걍 생략하지?
  IO(Tcp) ! Bind(self, new InetSocketAddress( "localhost", 0))

  override def receive: PartialFunction[Any, Unit] = {
    // Bound message receive 서버가 접속을 받을 준비가 되었다는 신호 수신
    case b @ Bound(localAddress) =>
      // do some logging or setup
    case CommandFailed(_: Bind) => context stop self
    // client와 연결 성공시
    case c @ Connected(remote, local) =>
      val handler = context.actorOf(Props[SimplisticHandler])
      val connection = sender() // client actor
      connection ! Register(handler)
  }
}

class SimplisticHandler extends Actor {
  import Tcp._

  override def receive: PartialFunction[Any, Unit] = {
    case Received(data) => sender() ! Write(data) // 되돌려준다. 실제로는 처리해서 DB에 쓰거나 파일에 쓰거나 기타등등
    // PeerClosed : 이런건 akka.io.Tcp 내용을 모르면 못쓰잖아?
    // TODO : 나중에 위키로 ㄱ
    /**
      * 고로 내용 요약한다.
      * Closing connections ( akka.io.Tcp )
      *   Close : FIN message를 보내는 역할을 해서 연결을 닫음, 원격으로부터 확인 기다리지 않음, 보류해놓은 쓰기
      *     데이터는 어쨌든 flush, 연결 닫힘이 성공시, listener는 Closed를 고지받음
      *   ConfirmedClose : FIN message를 보내는 것으로 전송하는 방향의 연결을 닫음. 그러나 원격 지점이 연결을 닫을
      *     때 까지는 계속 해서 데이터를 수신함. 보류된 쓰기는 flush 될 것이며 만약 연결 닫힘이 성공하면
      *     listener는 ConfirmedClosed를 고지받음
      *   Abort : RST message를 원격 지점에 보내는 것으로 즉시 연결 종료. 남은 쓰기 작업은 flush 되지 못함,
      *     만약 닫힘이 성공하면 listener는 Aborted를 고지 받음
      *   PeerClosed : 연결이 원격 지점에 의해 닫히면 listener에 의해 보내짐. 디폴트에서 연결은 자동적으로 닫히지만
      *     half-closed 연결을 지원하기 위해 Register message에 keepOpenOnPeerClosed가 true로 설정되었다면
      *     이 상태에서는 위의 close 명령 중 하나를 받을 때까지 열려있게될것임.. ㅇㅇ 아무튼 그렇다고함
      *   ErrorClosed : listener에게 에러가 발생 할 때마다 보내지며 강제로 연결이 닫힘
      *
      * Writing to a connection ( akka.io.Tcp )
      * 일단 접속이 맺어지면 데이터는 Tcp.WriteCommand 라는 형태의 Actor로부터 보내질 수 있다.
      * Tcp.WriteCommand는 3가지의 구체 구현을 가지고 있는 추상 클래스
      *   Tcp.Write : 가장 단순한 WriteCommand 구현이고 ByteString 인스턴스와 "ack" 이벤트를 감쌈
      *     ByteString은 최대 2GB의 변경불가능한 인메모리 데이터 하나 또는 더 많은 청크를 모델링한 것
      *   Tcp.WriteFile : 파일로부터의 law data 전송시 매우 효율적
      *     JVM 메모리로 모두를 적재할 필요 없이 송신가능토록 on-disk byte (contiguous) 청크 지정 가능
      *     Tcp.WriteFile은 원하면 2GB 보다 많은 데이터와 "ack" event를 "hold" 할 수 있음
      *   Tcp.CompoundWrite : 그룹핑 하거나 엮고 싶을 때
      *     위의 두 명령이 최소단위로 실행될때 하나로 묶어 한방에 처리 함으로써 오버헤드 최소화
      *     자세한 설명은 생략한다... 너무 길어...
      */
    case PeerClosed => context stop self
  }
}
