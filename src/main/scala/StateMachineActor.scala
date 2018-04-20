import akka.actor.{Actor, Props}
import akka.event.Logging
import mySystem._

import scala.collection.mutable
import scala.io.Source

/**
  * become !!!
  */
class CountdownActor extends Actor {
  private val log = Logging(context.system, this)
  private val MAX_COUNT = 10
  private var n = MAX_COUNT

  // dont!! like this
  /*
  override def receive: PartialFunction[Any, Unit] = if (n > 0) {
    case "count" =>
      n -= 1
  } else PartialFunction.empty
  */
  def counting: Actor.Receive = {
    case "count" =>
      n -= 1
      log.info(s"n = $n")
      if (n == 0) context.become(done)
  }

  def done: PartialFunction[Any, Nothing] = PartialFunction.empty

  def receive: Actor.Receive = counting
}

class DictionaryActor extends Actor {
  private val log = Logging(context.system, this)
  private val dictionary = mutable.Set[String]()

  override def receive: PartialFunction[Any, Unit] = uninitialized

  // uninitialized state
  def uninitialized: PartialFunction[Any, Unit] = {
    // received Init then become initialized state become
    case DictionaryActor.Init(path) =>
      //val stream = getClass.getResourceAsStream(path)
      //val words: BufferedSource = Source.fromInputStream(stream)
      val words = Source.fromFile("C:/smartPlatform/geneRestApiServer/words.txt")
      for (w <- words.getLines) dictionary += w
      context.become(initialized)
  }

  // initialized state
  def initialized: PartialFunction[Any, Unit] = {
    case DictionaryActor.IsWord(w) =>
      log.info(s"word '$w' exist: ${dictionary(w)}")
    // if received End message then become uninitialized state
    case DictionaryActor.End =>
      dictionary.clear
      context.become(uninitialized)
  }
5
  // unhandled massage
  override def unhandled(msg: Any): Unit = {
    log.info(s"message $msg should not be send in this state.")
  }
}

object DictionaryActor {
  // case class  < http://wiki.ucsit.co.kr/dokuwiki/doku.php?id=case_class >
  case class Init(path: String)

  case class IsWord(w: String)

  // TODO : case object?
  case object End

}

object ActorsBecome extends App {
  private val SLEEP_TIME = 1000
  val dict = ourSystem.actorOf(Props[DictionaryActor], "dictionary")
  dict ! DictionaryActor.IsWord("program")
  Thread.sleep(SLEEP_TIME)
  dict ! DictionaryActor.Init("/words.txt")
  Thread.sleep(SLEEP_TIME)
  dict ! DictionaryActor.IsWord("program")
  Thread.sleep(SLEEP_TIME)
  dict ! DictionaryActor.IsWord("balban")
  Thread.sleep(SLEEP_TIME)
  dict ! DictionaryActor.End
  Thread.sleep(SLEEP_TIME)
  dict ! DictionaryActor.IsWord("termination")
  Thread.sleep(SLEEP_TIME)
  ourSystem.terminate()
}