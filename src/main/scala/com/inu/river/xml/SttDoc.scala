package com.inu.river.xml

import org.json4s._
import org.json4s.native.JsonMethods._

/**
  * Created by henry on 6/2/16.
  */
case class SttDoc() {


}

trait JSONValue[A] {
  def map3(a: A): JValue
}

object JSONField {
  import org.json4s.JsonDSL._
  implicit val sentenceJV = new JSONValue[Sentence] {
    def map3(a: Sentence): JValue = {
      val Sentence(prefix, content, head +: _) = a
      s"$prefix-$head $content"
    }
  }
  implicit val roleJV = new JSONValue[Role] {
    def map3(a: Role): JValue = {
      val Role(field, sentences) = a
      field -> sentences.map(sentenceJV.map3)
    }

  }

}

object SttDoc {

}


