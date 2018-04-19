import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

class Hello(val hello: String) extends Actor {

  var log = Logging(context.system, this)

  def receive = {
    case `hello` =>
      log.info(s"Received a $hello")
    case msg =>
      log.info(s"Unexpected message '$msg'")
    context.stop(self)
  }
}

class Fuck extends Actor {

  val fuck = "This is fuck!"

  def receive = {
    case msg =>
      // make hello
      val helloFuckActor = context.actorOf(Props(new Hello(fuck)))
      helloFuckActor ! msg
  }
}

class ParentActor extends Actor {

  val log = Logging(context.system, this)

  def receive = {
    case "create" =>
      context.actorOf(Props[ChildActor])
      log.info(s"created a new Child - children = ${context.children}")
    case "hi" =>
      log.info("Kids, say hi!")
      for(c <- context.children) c ! "hi"
    case "stop" =>
      log.info("parent stopping")
      context.stop(self)
  }

}

class ChildActor extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "hi" =>
      val parent = context.parent
      log.info(s"my parent $parent made me say hi!")
  }

  override def postStop(): Unit = {
    log.info("child stopped!")
  }
}


object ActorCreate extends App {

  // create actor system
  val mySystem = ActorSystem("mySystem")

  // create actor by actorSystem, type is ActorRef
  val hiActor: ActorRef = mySystem.actorOf(Props(new Hello("hi")), name = "greeter")

  // 나의 액터와 교신 send 'hi' message
  hiActor ! "hi"
  hiActor ! 3

  val parent = mySystem.actorOf(Props[ParentActor], "parent")

  parent ! "create"
  parent ! "create"

  Thread.sleep(1000)
  parent ! "hi"
  Thread.sleep(1000)
  parent ! "stop"
  Thread.sleep(1000)
  mySystem.terminate()

  /*val fuckActor: ActorRef = mySystem.actorOf(Props(new Fuck()), name = "fuckActor")

  fuckActor ! "fuck"
  fuckActor ! "This is fuck!"*/

  // terminate myActorSystem
  //mySystem.terminate()
}
