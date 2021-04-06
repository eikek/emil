package emil.javamail.conv

import cats.data.{Validated, ValidatedNec}
import cats.implicits._
import emil.MimeType
import emil.javamail.internal.EnumerationConverter._
import jakarta.activation.{MimeTypeParameterList, MimeTypeParseException}

object MimeTypeDecode {

  def fromJavax(jm: jakarta.activation.MimeType): MimeType = {
    def paramMap(ps: MimeTypeParameterList): Map[String, String] =
      ps.getNames.asScalaList
        .map(n => (n.toString, Option(ps.get(n.toString)).getOrElse("")))
        .toMap
    MimeType(jm.getPrimaryType, jm.getSubType, paramMap(jm.getParameters))
  }

  def parse(str: String): Either[String, MimeType] =
    Either
      .catchNonFatal(new jakarta.activation.MimeType(str))
      .leftMap(_.getMessage)
      .map(fromJavax)

  def parseValidated(str: String): ValidatedNec[MimeTypeParseException, MimeType] =
    Validated
      .catchOnly[MimeTypeParseException](new jakarta.activation.MimeType(str))
      .toValidatedNec
      .map(fromJavax)

}
