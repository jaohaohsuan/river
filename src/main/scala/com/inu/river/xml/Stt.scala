package com.inu.river.xml

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormatterBuilder

import scala.xml.{Elem, Node, NodeSeq}

/**
  * Created by henry on 5/30/16.
  */

case class Role(name: String, sentences: Seq[Sentence])
case class Dialogs(sentences: Seq[Sentence])
case class Vtt(sentences: Seq[Sentence])

case class Sentence(id: String, text: String, times: Seq[Int], role: String)

object Element {

  object Role {

    val agentRole = """[R,r]1""".r
    val customerRole = """[R,r]0""".r

    def unapply(arg: Elem): Option[(String, Seq[Sentence])] = {
      for {
        attr <- arg.attributes.get("Name")
        rN = attr.text.trim()
        id = rN match {
          case agentRole() => "agent0"
          case customerRole()  => "customer0"
          case _ => ""
        }
        if id.nonEmpty
        sentences: Seq[Sentence] = (arg \\ "Item").map { case Item(text, times) => Sentence(id, text, times, rN) }
        if sentences.nonEmpty
      } yield (id, sentences)
    }

    object Item {
      def unapply(arg: Node): Option[(String, Seq[Int])] =
      (for {
        literal <- """\d+""".r findAllIn (arg \ "Time").text
        if !literal.isEmpty
        num = literal.toInt
      } yield num).toSeq match {
        case Nil => None
        case times => Some(((arg \ "Text").text, times))
      }
    }
  }

}


object Stt {

  def getRecognizeTextNode(ns: NodeSeq): Exception Either Node =
    ns \\ "Subject" find { n => (n \ "@Name").text == "RecognizeText" } match {
      case None => Left(new Exception("There is no element like: '<Subject Name=\"RecognizeText\">'"))
      case Some(ns) => Right(ns)
    }

  def getRoles(node: Node): Exception Either List[Role] = {
    node.child.collect { case Element.Role(name, sentences) => Role(name, sentences) } match {
      case Nil  => Left(new Exception("No Roles! Nonsense"))
      case roles =>
        //Role("dialogs", roles.flatMap{ e => e.sentences }.sortBy { case Sentence(_, _, x :: xs) => x })
        Right(roles.toList)
    }
  }

  def getDateTime(ns: NodeSeq, attribute: String): Exception Either DateTime = {
    ns \\ attribute headOption match {
      case None => Left(new Exception(s"Element '$attribute' missing"))
      case Some(dt) if dt.text.trim.isEmpty => Left(new Exception(s"'$attribute' value is empty"))
      case Some(dt) => try {
        import org.joda.time.format._
        //DateTimeZone.setDefault(DateTimeZone.forID("Asia/Taipei"))
        Right(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(dt.text.trim).withZone(DateTimeZone.forID("Asia/Taipei")))
      }
      catch {
        case e: IllegalArgumentException => Left(e)
        case e: UnsupportedOperationException => Left(e)
      }
    }
  }

  def getUserInfo(ns: NodeSeq, elementName: String): Exception Either String = {
     ns \\ "USER_INFO" \ elementName headOption match {
      case None => Right("")
      case Some(el) => Right(el.text.trim())
    }
  }

  def getAudioDuration(ns: NodeSeq): Exception Either Long = {
    ns \\ "AUDIO_INFO" \ "AUDIO_DURATION" headOption match {
      case None => Left(new Exception(s"AUDIO_DURATION can not find"))
      case Some(el) => try {
        Right((el.text.trim().toDouble * 1000).toLong)
      }
      catch {
        case ex: Exception => Left(ex)
      }
    }
  }

  def getAudioInfo(ns: NodeSeq, elementName: String): Exception Either String = {
    ns \\ "AUDIO_INFO" \ elementName headOption match {
      case None => Right("")
      case Some(el) => Right(el.text.trim)
    }
  }

  def getConversationInfo(ns: NodeSeq, key: String): Exception Either String = {
    val result = for {
      n <- ns \ "CONVERSATION_INFO"
      el <- n \ "CONVERSATION_ATTRIBUTE_KEY"
      value <- n \ "CONVERSATION_ATTRIBUTE_VALUE"
      if el.text.trim == key
    } yield value.text.trim
    result.headOption.map(Right(_)).getOrElse(Right(""))
  }

  def asIndex(date: DateTime): Exception Either String = {
    val fmt = new DateTimeFormatterBuilder()
      .appendLiteral("logs-")
      .appendYear(4,4)
      .appendLiteral('.')
      .appendMonthOfYear(2)
      .appendLiteral('.')
      .appendDayOfMonth(2)
      .toFormatter

     if ("""^logs-\d{4}\.\d{2}\.\d{2}$""".r.pattern.matcher(date.toString(fmt)).matches)
       Right(date.toString(fmt))
     else
      Left(new Exception(s"$date mismatching 'logs-yyyy.MM.dd'"))
  }

}