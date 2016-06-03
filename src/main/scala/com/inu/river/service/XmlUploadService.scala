package com.inu.river.service

import com.inu.river.xml.{JSONField, Role}
import org.json4s.JValue
import org.json4s.JsonAST.{JNull, JObject}
import spray.http.{BodyPart, ContentType, HttpEntity, MultipartContent}
import spray.httpx.unmarshalling.{BasicUnmarshallers, ContentExpected, DeserializationError, MalformedContent, UnsupportedContentType}
import spray.routing._
import spray.http.MediaTypes._
import spray.http.HttpCharsets._
import scala.util.Either
import scala.xml.NodeSeq

/**
  * Created by henry on 6/2/16.
  */
trait XmlUploadService extends Directives {

  import cats.std.all._
  import cats.syntax.traverse._
  import shapeless._

  val SttFiles: Directive1[Either[DeserializationError, List[NodeSeq]]] = entity(as[MultipartContent]).hmap {
    case MultipartContent(parts) :: HNil =>
      val `type=application/xml; charset=utf-8` = ContentType(`application/xml`, `UTF-8`)
      parts.map {
        case BodyPart(entity: HttpEntity.NonEmpty, _) =>
          BasicUnmarshallers.NodeSeqUnmarshaller(entity) }
          .toList.sequenceU
  }

  val CoNodeSeq: Directive[String :: JValue :: HNil ] = SttFiles.flatMap {
    case Left(ContentExpected)                   => reject(RequestEntityExpectedRejection)
    case Left(UnsupportedContentType(supported)) => reject(UnsupportedRequestContentTypeRejection(supported))
    case Left(MalformedContent(errorMsg, cause)) => reject(MalformedRequestContentRejection(errorMsg, cause))
    case Right(Nil) => reject
    case Right(ns) => {
      import com.inu.river.xml.Stt._
      val combined = ns.reduce(_ ++ _)
      val result = for {
        node <- getRecognizeTextNode(combined).right
        roles <- getRoles(node).right
        date <- getStartDateTime(combined).right
        indexName <- asIndex(date).right
      } yield indexName :: roles :: HNil

      result match {
        case Left(ex) =>
          reject(ValidationRejection(ex.getMessage))
        case Right(xs) =>
        val index :: roles :: HNil = xs

        val json = roles.map{ e => JSONField.roleJV.map3(e) }.foldLeft(JObject()){
            case (doc, o: JObject) => doc merge o
          }
          import cats._
          import cats.implicits._

          // Foldable[Seq].fold(roles)

          //Foldable[Seq].foldMap(xs)(JSONField.roleJV.map3)
          hprovide(index :: json :: HNil)
      }
    }
  }
}
