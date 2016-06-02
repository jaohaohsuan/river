package com.inu.river.service

import com.inu.river.xml.Role
import spray.http.{BodyPart, MultipartContent}
import spray.httpx.unmarshalling.{BasicUnmarshallers, ContentExpected, DeserializationError, MalformedContent, UnsupportedContentType}
import spray.routing._

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
      parts.map { case BodyPart(entity, _) =>
        BasicUnmarshallers.NodeSeqUnmarshaller(entity) }.toList.sequenceU
  }

  val CoNodeSeq: Directive[String :: Seq[Role] :: HNil ] = SttFiles.flatMap {
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
          hprovide(xs)
      }
    }
  }
}
