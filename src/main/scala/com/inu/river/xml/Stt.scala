package com.inu.river.xml

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder

import scala.xml.{Elem, Node, NodeSeq}

/**
  * Created by henry on 5/30/16.
  */

case class Role(name: String, sentences: Seq[Sentence])

case class Sentence(id: String, text: String, times: Seq[Int])

object Element {

  object Role {

    val agentRole = """[R,r]0""".r
    val customerRole = """[R,r]1""".r

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
        sentences: Seq[Sentence] = (arg \\ "Item").map { case Item(text, times) => Sentence(id, text, times) }
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

  def getRoles(node: Node): Exception Either Seq[Role] = {
    node.child.collect { case Element.Role(name, sentences) => Role(name, sentences) } match {
      case Nil  => Left(new Exception("No Roles! Nonsense"))
      case roles => Right(roles)
    }
  }

  def getStartDateTime(ns: NodeSeq): Exception Either DateTime = {
    ns \\ "START_DATETIME" headOption match {
      case None => Left(new Exception("Element 'START_DATETIME' missing"))
      case Some(dt) if dt.text.trim.isEmpty => Left(new Exception("'START_DATETIME' value is empty"))
      case Some(dt) => try {
        import org.joda.time.format._
        Right(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(dt.text.trim))
      }
      catch {
        case e: IllegalArgumentException => Left(e)
        case e: UnsupportedOperationException => Left(e)
      }
    }
  }

  def asIndex(date: DateTime): Exception Either String = {
    val fmt = new DateTimeFormatterBuilder()
      .appendLiteral("log-")
      .appendYear(4,4)
      .appendLiteral('.')
      .appendMonthOfYear(2)
      .appendLiteral('.')
      .appendDayOfMonth(2)
      .toFormatter

     if ("""^log-\d{4}\.\d{2}\.\d{2}$""".r.pattern.matcher(date.toString(fmt)).matches)
       Right(date.toString(fmt))
     else
      Left(new Exception(s"$date mismatching 'log-yyyy.MM.dd'"))
  }

}