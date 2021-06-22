package emil.doobie

import cats.Show
import cats.implicits._
import doobie.util.invariant._
import doobie.{Meta, Read, Write}
import emil._
import emil.doobie.EmilDoobieMeta._
import emil.javamail.syntax._
import org.tpolecat.typename.TypeName

trait EmilDoobieMeta {

  implicit val sslTypeMeta: Meta[SSLType] =
    Meta[String].timap(parseEnumOrThrow(SSLType.fromString))(_.name)

  implicit val mimeTypeMeta: Meta[MimeType] =
    Meta[String].timap(parseOrThrow(MimeType.parse))(_.asString)

  implicit val mailAddressMeta: Meta[MailAddress] =
    Meta[String].timap(parseOrThrow(MailAddress.parse))(_.asUnicodeString)

  val mailAddressMulticolumnRead: Read[MailAddress] =
    Read[(Option[String], String)]
      .map(parseOrThrow({ case (n, a) => MailAddress.parseAddressAndName(n, a) }))

  val mailAddressMulticolumnWrite: Write[MailAddress] =
    Write[(Option[String], String)].contramap { mailAddress =>
      (mailAddress.name, mailAddress.address)
    }

  implicit val mailAddressListMeta: Meta[List[MailAddress]] =
    Meta[String].timap(parseOrThrow(MailAddress.parseMultiple))(
      _.map(_.asUnicodeString).mkString(",")
    )
}

object EmilDoobieMeta extends EmilDoobieMeta {

  private def parseOrThrow[S: Show, A](
      f: S => Either[String, A]
  )(str: S)(implicit ev: TypeName[A]): A =
    f(str) match {
      case Right(a)  => a
      case Left(err) => throw InvalidValue[S, A](str, err)
    }

  private def parseEnumOrThrow[A](
      f: String => Either[String, A]
  )(str: String)(implicit ev: TypeName[A]): A =
    f(str) match {
      case Right(a) => a
      case Left(_)  => throw InvalidEnum[A](str)
    }
}
