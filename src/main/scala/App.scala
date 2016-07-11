package com.inu.river

import java.net.InetAddress
import java.util.Calendar

import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.{Config, ConfigFactory}
import spray.can.Http
import service.HttpService
import akka.pattern.ask
import akka.util.Timeout
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.joda.time.DateTime


object Main extends App {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  val config: Config = ConfigFactory.load()

  val settings = org.elasticsearch.common.settings.Settings.settingsBuilder()
    .put("cluster.name", config.getString("elasticsearch.cluster-name"))
    //.put("client.transport.sniff", true)
    .build()

  val esAddr = InetAddress.getByName(config.getString("elasticsearch.transport-address"))
  val esTcpPort = config.getInt("elasticsearch.transport-tcp")

  val client = TransportClient.builder().settings(settings).build()
    .addTransportAddress(new InetSocketTransportAddress(esAddr, esTcpPort))

  val status = client.admin().cluster().prepareHealth().get().getStatus

  println(status)

  val listener = system.actorOf(Props(classOf[HttpService], client), "service")

  val host = "0.0.0.0"
  val port = ConfigFactory.load().getInt("http.port")

  val release = () => {
    client.close()
    system.shutdown()
  }

  System.out.print(s"${Calendar.getInstance().getTimeZone}")
  System.out.print(s"${Calendar.getInstance().getTime}")

  IO(Http).ask(Http.Bind(listener, interface = host, port = port))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"river service v${com.inu.river.BuildInfo.version} bound to $address")
      case Http.CommandFailed(cmd) =>
        println("river service could not bind to " +  s"$host:$port, ${cmd.failureMessage}")
        release()
    }

  sys.addShutdownHook(release())
}
