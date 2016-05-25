package com.grandsys.river
/**
  * Created by henry on 5/26/16.
  */
import spray.routing._
import spray.routing.authentication.BasicAuth
import spray.http.{StatusCodes}

class SkHttpService extends HttpServiceActor {
  implicit val executionContext = actorRefFactory.system.dispatcher
  def receive = runRoute {
    path("ping") {
      get {
        complete("pong")
      }
    } ~
      path("stt" / "ami") {
        authenticate(BasicAuth("sk")) { usr =>
          complete(StatusCodes.OK)
        }
      }
  }
}
