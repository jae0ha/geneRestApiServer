import DownloadManager.Download
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.event.Logging
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.Source
import mySystem._

/**
  * A Downloader actor
  *
  * -> receive DownloadManager.Download(url, dest)
  * -> download from url
  * -> write to dest
  * -> send to sender 'finished'
  */
class Downloader extends Actor {
  override def receive: PartialFunction[Any, Unit] = {
    case DownloadManager.Download(url, dest) =>
      val content = Source.fromURL(url)
      FileUtils.writeStringToFile(new java.io.File(dest), content.mkString)
      sender ! DownloadManager.Finished(dest)
  }
}

/**
  * A DownloadManager actor
  *
  * Queue
  * downloaders[ActorRef] : ActorRef Queue
  * pendingWork[DownloadManager.Download()] : DownloadManager object Download class
  * Map
  * workItems Map[ActorRef, DownloadManager.Download]
  *
  * @param downloadSlots : downloaders
  */
class DownloadManager(val downloadSlots: Int) extends Actor {
  val log = Logging(context.system, this)
  val downloaders = mutable.Queue[ActorRef]()
  val pendingWork = mutable.Queue[DownloadManager.Download]()
  val workItems = mutable.Map[ActorRef, DownloadManager.Download]()

  /** make downloader actor when preStart and enqueue to downloaders queue */
  override def preStart(): Unit = {
    for (i <- 0 until downloadSlots)
      downloaders.enqueue(context.actorOf(Props[Downloader], s"downloader$i"))
  }

  /** if pendingwork exist and downloader is in downloaders queue
    * then dequeue downloader, Download() and send message(DownloadManager.Download()) to downloader
    * and put map downloader(key), work(val)
    */
  private def checkMoreDownloads(): Unit = {
    if (pendingWork.nonEmpty && downloaders.nonEmpty) {
      val dl = downloaders.dequeue()
      val workItem = pendingWork.dequeue()
      log.info(s"$workItem starting, ${downloaders.size} download slots left")
      dl ! workItem
      workItems(dl) = workItem
    }
  }

  /**
    * received message pattern is DownloadManager.Download then
    * received work(downlaoad) enqueue pendingWorkQueue
    * call checkMoreDownloads
    * receive Finished message from downloader
    * remove sender(downloader) from map
    * enqueue downloadersqueue
    * call checkMoreDownloads
    *
    * @return
    */
  override def receive: PartialFunction[Any, Unit] = {
    // @ : variable binding on pattern matching
    case msg@DownloadManager.Download(url, dest) =>
      pendingWork.enqueue(msg)
      checkMoreDownloads()
    // TODO : why not using @ ???
    case DownloadManager.Finished(dest) =>
      workItems.remove(sender)
      downloaders.enqueue(sender)
      log.info(s"Down to '$dest' finishied, ${downloaders.size} down slots left")
      checkMoreDownloads()
  }

  /**
    * Supervisor strategy
    * FileNotFoundException : resume
    * else : escalate
    */
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 6, withinTimeRange = 30 seconds) {
      case fnf: java.io.FileNotFoundException =>
        log.info(s"Resource could not be found : $fnf")
        workItems.remove(sender)
        downloaders.enqueue(sender)
        Resume
      case _ =>
        Escalate
    }
}

/**
  * Companion Object
  * why not val?
  */
object DownloadManager {

  // TODO : meaning of case
  case class Download(url: String, dest: String)

  case class Finished(dest: String)

}

object SupervisionDownloader extends App {
  val manager = ourSystem.actorOf(Props(classOf[DownloadManager], 4), "manager")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec.txt")
  //Thread.sleep(1000)
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  manager ! Download("https://github.com/scala/scala/blob/2.13.x/README.md", "README.md")
  Thread.sleep(9000)
  ourSystem.stop(manager)
  Thread.sleep(9000)
  ourSystem.terminate()
}