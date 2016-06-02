package com.inu.river.xml

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder

import scala.xml.{Elem, Node, NodeSeq}

/**
  * Created by henry on 5/30/16.
  */
trait Stt[T] {
  def subject(): T
}

case class Role(name: String, items: NodeSeq)

object RoleN {

  def unapply(arg: Elem): Option[(String, NodeSeq)] = {
    arg.attributes.get("Name").map(a => (a.text.trim, arg \\ "Item"))
  }
}

object Stt {

  def getRecognizeTextNode(ns: NodeSeq): Exception Either Node =
    ns \\ "Subject" find { n => (n \ "@Name").text == "RecognizeText" } match {
      case None => Left(new Exception("There is no element like: '<Subject Name=\"RecognizeText\">'"))
      case Some(ns) => Right(ns)
    }

  def getRoles(node: Node): Exception Either Seq[Role] = {
    node.child.collect { case RoleN(n, items) => Role(n, items) } match {
      case Nil  => Left(new Exception("No Roles! Nonsense"))
      case roles => Right(roles)
    }
  }

  def getStartDateTime(ns: NodeSeq): Exception Either DateTime = {
    import org.joda.time.format._

    ns \\ "START_DATETIME" headOption match {
      case None => Left(new Exception("Element 'START_DATETIME' missing"))
      case Some(dt) if dt.text.trim.isEmpty => Left(new Exception("'START_DATETIME' value is empty"))
      case Some(dt) => try {
        Right(DateTime.parse(dt.text.trim))
        //Right(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(dt.text.trim))
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