package com.inu.river.service
/**
  * Created by henry on 5/26/16.
  */

import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.action.{ActionListener, ListenableActionFuture}
import org.json4s.JValue
import org.json4s.native.JsonMethods._
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.Json4sSupport
import spray.routing._

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success }


class SkHttpService(val client: org.elasticsearch.client.Client) extends HttpServiceActor with XmlUploadService with Json4sSupport {

  import StatusCodes._

  implicit val executionContext = context.dispatcher
  implicit val json4sFormats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  implicit class ActionListenableFutureConverter[T](x: ListenableActionFuture[T]) {
    def future: Future[T] = {
      val p = Promise[T]()
      x.addListener(new ActionListener[T] {
        def onFailure(e: Throwable) = p.failure(e)
        def onResponse(response: T) = p.success(response)
      })
      p.future
    }
  }

  def write(doc: String,index: String,id: String): Future[UpdateResponse] = client.prepareUpdate(index, "ami", id).setDoc(doc).setUpsert(doc).execute().future

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
                  onComplete(write(compact(render(doc)), index, s"$uuid")) {
                    case scala.util.Success(res) => complete(OK, s"""{ "acknowledged": true, "created" : ${res.isCreated} }""")
                    case Failure(ex) => complete(BadRequest, s"""{
                                                                 |  "error" :
                                                                 |  {
                                                                 |    "title" : "prepareUpdate"
                                                                 |    "message" : "${ex.getMessage}"
                                                                 |  }
                                                                 |}""".stripMargin)
                  }
                }
              }
            }
            //}
        }
      }
  }
}
