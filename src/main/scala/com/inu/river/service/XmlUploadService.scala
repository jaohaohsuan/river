package com.inu.river.service

import com.inu.river.xml.{Dialogs, JSONValue, Sentence, Vtt}
import spray.http.{BodyPart, ContentType, HttpEntity, MultipartContent}
import spray.httpx.unmarshalling.{BasicUnmarshallers, ContentExpected, DeserializationError, MalformedContent, UnsupportedContentType}
import spray.routing._
import spray.http.HttpCharsets._
import scala.util.Either
import scala.xml.NodeSeq

trait XmlUploadService extends Directives {

  import cats.std.all._
  import cats.syntax.traverse._
  import shapeless._
  import org.json4s._

  val SttFiles: Directive1[Either[DeserializationError, List[NodeSeq]]] = entity(as[MultipartContent]).hmap {
    case MultipartContent(parts) :: HNil =>
      (for {
        BodyPart(entity@HttpEntity.NonEmpty(ContentType(mediaType, _), data), _) <- parts
        ns = BasicUnmarshallers.NodeSeqUnmarshaller(entity.copy(contentType = ContentType(mediaType, `UTF-8`)))
      } yield ns).toList.sequenceU
  }

  val SttDoc: Directive[String :: JValue :: HNil ] = SttFiles.flatMap {
    case Left(ContentExpected)                   => reject(RequestEntityExpectedRejection)
    case Left(UnsupportedContentType(supported)) => reject(UnsupportedRequestContentTypeRejection(supported))
    case Left(MalformedContent(errorMsg, cause)) => reject(MalformedRequestContentRejection(errorMsg, cause))
    case Right(Nil) => reject()
    case Right(ns) => {
      import com.inu.river.xml.Stt._
      import cats._
      import com.inu.river.xml.json4sImplicits._
      import com.inu.river.xml.JSONValue._

      def toJson[A: JSONValue](la: List[A]) = Foldable[List].foldMap(la)(implicitly[JSONValue[A]].marshalling)

      val combined = ns.reduce(_ ++ _)
      val result = for {
        node <- getRecognizeTextNode(combined).right
        roles <- getRoles(node).right
        date <- getStartDateTime(combined).right
        indexName <- asIndex(date).right
        mixed <- Right(roles.flatMap{ r => r.sentences }.sortBy{ case Sentence(_, _, begin +: _, _) => begin }).right
      } yield indexName :: toJson(roles).merge(toJson(Dialogs(mixed) :: Nil)).merge(toJson(Vtt(mixed) :: Nil))  :: HNil

      result match {
        case Left(ex) => reject(ValidationRejection(ex.getMessage))
        case Right(xs) => hprovide(xs)
      }
    }
  }


}
