package com.inu.river.xml

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

  def getStartDateTime(ns: NodeSeq): Option[String] = (ns \\ "START_DATETIME").map(_.text.trim()).headOption

}