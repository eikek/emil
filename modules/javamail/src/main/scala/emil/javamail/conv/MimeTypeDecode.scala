package emil.javamail.conv

import javax.activation.{MimeTypeParameterList, MimeTypeParseException}

import cats.data.{Validated, ValidatedNec}
import cats.implicits._
import emil.MimeType
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

  def parseValidated(str: String): ValidatedNec[MimeTypeParseException, MimeType] =
    Validated
      .catchOnly[MimeTypeParseException](new javax.activation.MimeType(str))
      .toValidatedNec
      .map(fromJavax)

}
