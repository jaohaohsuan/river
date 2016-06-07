package com.inu.river.service
/**
  * Created by henry on 5/26/16.
  */

import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.Json4sSupport
import spray.routing._


class SkHttpService extends HttpServiceActor with XmlUploadService with Json4sSupport {

  import StatusCodes._

  implicit val json4sFormats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  def receive = runRoute {
    path("ping") {
      get {
        complete(OK, "pong")
      }
    } ~
      path("stt" / "ami" / JavaUUID ) { uuid =>
        put {
          //authenticate(BasicAuth("sk")) { usr =>
            SttDoc { (index, doc) =>
              respondWithHeader(RawHeader("Content-Location", s"$index/$uuid")) {
                respondWithMediaType(`application/json`) {
                  complete(OK, doc)
                }
              }
            }
            //}
        }
      }
  }
}
