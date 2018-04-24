import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.Logging
import akka.io.{IO, Tcp}
import akka.util.ByteString

/* Ack based buffering tcp actor example */
class AckBasedWriteServer extends Actor {

  import Tcp._
  import context.system

  val log = Logging(context.system, this)

  IO(Tcp) ! Bind(self, (new InetSocketAddress("localhost", 0)))

  override def receive: PartialFunction[Any, Unit] = {
    case b @ Bound(localAddress) =>
      log.info(s"Bound $localAddress")
    case CommandFailed(_: Bind) =>
      context stop self
    case Connected(remote, local) =>
      log.info(s"received connection from $remote")
      // TODO : val handler = context.actorOf(Props(handlerClass, sender(), remote)
      val handler = context.actorOf(Props[SimpleEchoHandler])
      sender ! Register(handler, keepOpenOnPeerClosed = true)
  }
}

class SimpleEchoHandler(connection: ActorRef, remote: InetSocketAddress)
  extends Actor with ActorLogging {

  import Tcp._

  private var storage = Vector.empty[ByteString]
  val maxStored = 100000000L
  val highWatermark = maxStored * 5 / 10
  val lowWatermark = maxStored / 10
  var stored = 0L
  var suspended = false
  var closing = false
  var transferred = 0L

  context watch connection

  case object Ack extends Event

  override def receive: PartialFunction[Any, Unit] = {
    case Received(data) =>
      buffer(data)
      connection ! Write(data, Ack)

      context.become({
        case Received(data) => buffer(data)
        case Ack => acknowledge()
        case PeerClosed => closing = true
      }, discardOld = false)

    case PeerClosed => context stop self
  }

  private def buffer(data: ByteString): Unit = {
    storage :+ data
    stored += data.size

    if(stored > maxStored) {
      log.warning(s"drop connection to [$remote](buffer overrun)")
      context stop self
    } else if (stored > highWatermark) {
      log.debug(s"suspending reading")
      connection ! SuspendReading
      suspended = true
    }
  }

  private def acknowledge(): Unit = {
    require(storage.nonEmpty, "storage was empty")

    val size = storage(0).size
    stored -= size
    transferred += size

    storage = storage drop 1

    if(suspended && stored < lowWatermark) {
      log.debug(s"resuming reading")
      connection ! ResumeReading
      suspended = false
    }

    if(storage.isEmpty) {
      if(closing) context stop self
      else context.unbecome()
    } else connection ! Write(storage(0), Ack)
  }
}
