package com.inu.river.service
/**
  * Created by henry on 5/26/16.
  */

import com.inu.river.xml.Role

import spray.http.HttpHeaders.RawHeader
import spray.http._
import spray.httpx.Json4sSupport
import spray.httpx.unmarshalling._
import spray.routing._
import spray.http.MediaTypes._

import scala.util.Either
import scala.xml.NodeSeq




class SkHttpService extends HttpServiceActor with XmlUploadService with Json4sSupport {

  import StatusCodes._
  import spray.routing._
  import org.json4s._
  import org.json4s.native.JsonMethods._
  import org.json4s.JsonDSL._

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
            CoNodeSeq { (index, nodes) =>
              respondWithHeader(RawHeader("Content-Location", s"$index/$uuid")) {
                respondWithMediaType(`application/json`) {

                  nodes.foreach(println)
                  complete(OK, ("acknowledged" -> "true"): JValue)
                }
              }
            }
            //}
        }
      }
  }
}
