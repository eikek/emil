package emil.javamail.conv

import cats.implicits._
import emil.MimeType
import javax.activation.MimeTypeParameterList
import emil.javamail.internal.EnumerationConverter._

object MimeTypeDecode {

  def fromJavax(jm: javax.activation.MimeType): MimeType = {
    def paramMap(ps: MimeTypeParameterList): Map[String, String] =
      ps.getNames.asScalaList
        .map(n => (n.toString, Option(ps.get(n.toString)).getOrElse("")))
        .toMap
    MimeType(jm.getPrimaryType, jm.getSubType, paramMap(jm.getParameters))
  }

  def parse(str: String): Either[String, MimeType] =
    Either
      .catchNonFatal(new javax.activation.MimeType(str))
      .leftMap(_.getMessage)
      .map(fromJavax)

}
