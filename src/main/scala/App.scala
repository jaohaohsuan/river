package com.inu.river

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.ConfigFactory
import spray.can.Http
import service.SkHttpService
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.BuildInfo

object Main extends App {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  val skListener = system.actorOf(Props(classOf[SkHttpService]), "service")

  val host = "0.0.0.0"
  val port = ConfigFactory.load().getInt("http.port")

  IO(Http).ask(Http.Bind(skListener, interface = host, port = port))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"river service v${com.inu.river.BuildInfo.version} bound to $address")
      case Http.CommandFailed(cmd) =>
        println("river service could not bind to " +  s"$host:$port, ${cmd.failureMessage}")
        system.shutdown()
    }
}
