package com.grandsys.river

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

object Main extends App {
  implicit val system = ActorSystem()

  val skListener = system.actorOf(Props(classOf[SkHttpService]), "service")

  IO(Http) ! Http.Bind(skListener, interface = "0.0.0.0", port = 7879)
}
