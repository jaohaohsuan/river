package com.inu.river


import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.ConfigFactory
import spray.can.Http
import service.SkHttpService

object Main extends App {
  implicit val system = ActorSystem()

  val skListener = system.actorOf(Props(classOf[SkHttpService]), "service")

  IO(Http) ! Http.Bind(skListener, interface = "0.0.0.0", port = ConfigFactory.load().getInt("http.port"))
}
