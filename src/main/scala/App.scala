package com.inu.river

import java.net.InetAddress

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.{Config, ConfigFactory}
import spray.can.Http
import service.SkHttpService
import akka.pattern.ask
import akka.util.Timeout
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

object Main extends App {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  val config: Config = ConfigFactory.load()

  val settings = org.elasticsearch.common.settings.Settings.settingsBuilder()
    .put("cluster.name", config.getString("elasticsearch.cluster-name")).build()

  val client = TransportClient.builder().settings(settings).build()
    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.getString("elasticsearch.transport-address")), 9300))

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
