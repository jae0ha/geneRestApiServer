import DownloadManager.Download
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.event.Logging
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.Source
import mySystem._

import scala.language.postfixOps

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
  * @param downloadSlots : downLoaders
  */
class DownloadManager(val downloadSlots: Int) extends Actor {
  val log = Logging(context.system, this)
  val downLoaders: mutable.Queue[ActorRef] = mutable.Queue[ActorRef]()
  val pendingWork: mutable.Queue[DownloadManager.Download] = mutable.Queue[DownloadManager.Download]()
  val workItems: mutable.Map[ActorRef, DownloadManager.Download] = mutable.Map[ActorRef, DownloadManager.Download]()

  /** make downloader actor when preStart and enqueue to downloaders queue */
  override def preStart(): Unit = {
    for (i <- 0 until downloadSlots)
      downLoaders.enqueue(context.actorOf(Props[Downloader], s"downloader$i"))
  }

  /** if pendingwork exist and downloader is in downloaders queue
    * then dequeue downloader, Download() and send message(DownloadManager.Download()) to downloader
    * and put map downloader(key), work(val)
    */
  private def checkMoreDownloads(): Unit = {
    if (pendingWork.nonEmpty && downLoaders.nonEmpty) {
      val dl = downLoaders.dequeue()
      val workItem = pendingWork.dequeue()
      log.info(s"$workItem starting, ${downLoaders.size} download slots left")
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
    case _@DownloadManager.Finished(dest) =>
      workItems.remove(sender)
      downLoaders.enqueue(sender)
      log.info(s"Down to '$dest' finishied, ${downLoaders.size} down slots left")
      checkMoreDownloads()
  }

  val MAX_NR_OF_RETRIES = 6
  val WITHIN_TIME_RANGE = 30

  /**
    * Supervisor strategy
    * FileNotFoundException : resume
    * else : escalate
    */
  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = MAX_NR_OF_RETRIES, withinTimeRange = WITHIN_TIME_RANGE seconds) {
      case fnf: java.io.FileNotFoundException =>
        log.info(s"Resource could not be found : $fnf")
        workItems.remove(sender)
        downLoaders.enqueue(sender)
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
  val DOWNLOAD_SLOTS = 4
  val SLEEP_TIME = 1000
  val manager = ourSystem.actorOf(Props(classOf[DownloadManager], DOWNLOAD_SLOTS), "manager")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec.txt")
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
  Thread.sleep(SLEEP_TIME)
  ourSystem.stop(manager)
  Thread.sleep(SLEEP_TIME)
  ourSystem.terminate()
}