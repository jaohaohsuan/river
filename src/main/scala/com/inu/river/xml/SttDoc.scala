package com.inu.river.xml

import cats._
import org.json4s.JsonAST.JObject
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection.+:

/**
  * Created by henry on 6/2/16.
  */

trait JSONValue[A] {
  def marshalling(a: A): JValue
}

object json4sImplicits {

  implicit val JValueMonoid = new Monoid[JValue] {
    def empty: JValue = JObject()
    def combine(x: JValue, y: JValue): JValue = x merge y
  }

}

object JSONValue {
  import org.json4s.JsonDSL._

  implicit val sentenceJV = new JSONValue[Sentence] {
    def marshalling(a: Sentence): JValue = {
      val Sentence(prefix, content, head +: _, _) = a
      s"$prefix-$head $content"
    }
  }
  implicit val roleJV = new JSONValue[Role] {
    def marshalling(a: Role): JValue = {
      val Role(field, sentences) = a
      field -> sentences.map{ e => sentenceJV.marshalling(e) }
    }
  }

  implicit val dialogsJV = new JSONValue[Dialogs] {
    def marshalling(a: Dialogs): JValue = {
      "dialogs" -> a.sentences.map { e => sentenceJV.marshalling(e) }
    }
  }

  implicit val vttJV = new JSONValue[Vtt] {
    def marshalling(a: Vtt): JValue = {

      def formatTime(value: Int) =
        new org.joda.time.DateTime(value, org.joda.time.DateTimeZone.UTC).toString("HH:mm:ss.SSS")

      "vtt" -> a.sentences.map { case Sentence(prefix, content, times, role) =>
        val (begin +: _) = times
        val (_ :+ end) = times
          s"""$prefix-$begin ${formatTime(begin)} --> ${formatTime(end)}\n<v $role>$content</v>\n"""
       }
    }
  }

}




