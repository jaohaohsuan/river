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

  def getRecognizeTextNode(ns: NodeSeq): Option[Node] =
    ns \\ "Subject" find { n => (n \ "@Name").text == "RecognizeText" }

  def getRoles(node: Node): Option[Seq[Role]] = Some(node.child.collect { case RoleN(n, items) => Role(n, items) })

  def getStartDateTime(ns: NodeSeq): Option[DateTime] = {
    import org.joda.time.format._
    def toDateTime(text: String):Option[DateTime] = {
      try {
        Some(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(text))
      }
      catch {
        case e: IllegalArgumentException => None
        case e: UnsupportedOperationException => None
      }
    }
    for {
     el <- (ns \\ "START_DATETIME").headOption
     dt <- toDateTime(el.text.trim)
    } yield dt
  }

  def asIndex(date: DateTime): Option[String] = {
    val fmt = new DateTimeFormatterBuilder()
      .appendLiteral("log-")
      .appendYear(4,4)
      .appendLiteral('.')
      .appendMonthOfYear(2)
      .appendLiteral('.')
      .appendDayOfMonth(2)
      .toFormatter

     if ("""^log-\d{4}\.\d{2}\.\d{2}$""".r.pattern.matcher(date.toString(fmt)).matches)
       Some(date.toString(fmt))
     else
      None
  }

}