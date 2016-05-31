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
import spray.routing.directives.RouteDirectives._
import spray.http.MediaTypes._
import org.joda.time.DateTime

import scala.util.Either
import scala.xml.NodeSeq


trait XmlUploadService extends Directives {

  import shapeless._
  import cats.std.all._
  import cats.syntax.traverse._

  val xmls: Directive1[Either[DeserializationError, List[NodeSeq]]] = entity(as[MultipartContent]).hmap {
    case MultipartContent(parts) :: HNil =>
      parts.map { case BodyPart(entity, _) => BasicUnmarshallers.NodeSeqUnmarshaller(entity) }.toList.sequenceU
  }

  val CoNodeSeq: Directive[String :: Seq[Role] :: HNil ] = xmls.flatMap {
    case Left(ContentExpected)                   => reject(RequestEntityExpectedRejection)
    case Left(UnsupportedContentType(supported)) => reject(UnsupportedRequestContentTypeRejection(supported))
    case Left(MalformedContent(errorMsg, cause)) => reject(MalformedRequestContentRejection(errorMsg, cause))
    case Right(Nil) => reject
    case Right(ns) => {
      import com.inu.river.xml.Stt._
      val sum = ns.reduce(_ ++ _)
      (for {
        node <- getRecognizeTextNode(sum)
        roles <- getRoles(node)
        date <- getStartDateTime(sum)
        indexName <- asIndex(date)
      } yield indexName :: roles :: HNil) match {
        case None =>
          reject(ValidationRejection("START_DATETIME or RecognizeText missing"))
        case Some(xs) =>
          hprovide(xs)
      }
    }
  }
}

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
                  complete(OK, ("acknowledged" -> "true"): JValue)
                }
              }
            }
            //}
        }
      }
  }
}
