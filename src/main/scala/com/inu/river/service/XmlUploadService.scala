package com.inu.river.service

import com.inu.river.xml.{Dialogs, JSONValue, Sentence, Vtt}
import org.joda.time.DateTime
import org.json4s.JsonAST.JValue
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

      def decomposeDate(start: DateTime, end: DateTime): JObject = {

        import org.json4s.JsonDSL._
          ("startTime" -> start.toString("yyyy-MM-dd'T'HH:mm:ssZ")) ~~
          ("endTime"   -> end.toString("yyyy-MM-dd'T'HH:mm:ssZ")) ~~
          ("year"      -> start.getYear) ~~
          ("quarter"   -> (start.getMonthOfYear + 2) / 3) ~~
          ("month"     -> start.getMonthOfYear) ~~
          ("weekNum"   -> start.getWeekOfWeekyear) ~~
          ("weekDay"   -> start.getDayOfWeek) ~~
          ("monthDay"  -> start.getDayOfMonth)
      }

      implicit val combined = ns.reduce(_ ++ _)
      val result = for {
        recognizeTextNode <- getRecognizeTextNode(combined).right
        silenceNode       <- getNode("Silence").right
        silences          <- getDurationsWithoutHead(silenceNode).right
        interruptionNode  <- getNode("Interruption").right
        interruptions     <- getDurations(interruptionNode).right
        roles             <- getRoles(recognizeTextNode).right
        start             <- getDateTime(combined, "START_DATETIME").right
        end               <- getDateTime(combined, "END_DATETIME").right
        length            <- getAudioDuration(combined).right
        agentPhoneNo      <- getAudioInfo(combined, "OPERATOR_PHONENUMBER").right
        endStatus         <- getAudioInfo(combined, "CONVERSATION_END_STATUS_TYPE_CD").right
        projectName       <- getAudioInfo(combined, "PROJECT_NAME").right
        agentId           <- getUserInfo(combined, "USER_ID").right
        agentName         <- getUserInfo(combined, "USER_NAME").right
        callDirection     <- getConversationInfo(combined, "amivoice.common.direction").right
        customerPhoneNo   <- getConversationInfo(combined, "amivoice.common.customer.phonenumber").right
        customerGender    <- getConversationInfo(combined, "amivoice.common.customer.gender").right
        indexName         <- asIndex(start).right
        mixed             <- Right(roles.flatMap { r => r.sentences }.sortBy { case Sentence(_, _, begin +: _, _) => begin }).right
      } yield {
        import org.json4s.JsonDSL._

       val longestMixedSilence = silences.filter{ case (name, durations) => name == "mix" }
         .map { case (_, durations) => durations.map(_.len).max }.headOption.getOrElse(0)

       val (interruptionInfo, r0r1TotalInterruptionLen) = interruptions.foldLeft((JObject(Nil), 0)){ case ((json, r0r1), (rN, durations)) =>
         val sum = durations.map(_.len).sum
         (json ~~ (s"${rN.toLowerCase}TotalInterruption" -> sum), r0r1 + sum)
       }

        val audioInfo: JObject = (("length" -> length) ~~
                                  ("endStatus" -> endStatus) ~~
                                  ("projectName" -> projectName) ~~
                                  ("agentPhoneNo" -> agentPhoneNo) ~~
                                  ("mixLongestSilence" -> longestMixedSilence))
          .merge(interruptionInfo ~~ ("sumTotalInterruption" -> r0r1TotalInterruptionLen))

        val userInfo = ("agentId" -> agentId) ~~
                       ("agentName" -> agentName)

        val conversationInfo = ("callDirection" -> callDirection) ~~
                               ("customerPhoneNo" -> customerPhoneNo) ~~
                               ("customerGender" -> customerGender)
 
        val extra = decomposeDate(start, end) ~~ audioInfo ~~ userInfo ~~ conversationInfo
        indexName :: toJson(roles).merge(toJson(Dialogs(mixed) :: Nil)).merge(toJson(Vtt(mixed) :: Nil)).merge(extra) :: HNil
      }
      result match {
        case Left(ex) => reject(ValidationRejection(ex.getMessage))
        case Right(xs) => hprovide(xs)
      }
    }
  }

}
