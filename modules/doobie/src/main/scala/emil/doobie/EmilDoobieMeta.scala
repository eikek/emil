package emil.doobie

import cats.effect._
import cats.implicits._
import doobie.util.invariant._
import doobie.{Meta, Read, Write}
import org.tpolecat.typename.TypeName
import emil._
import emil.doobie.EmilDoobieMeta.parseOrThrow
import emil.javamail.syntax._

trait EmilDoobieMeta {

  implicit val sslTypeMeta: Meta[SSLType] =
    Meta[String].imap(parseOrThrow(SSLType.fromString))(_.name)

  implicit val sslTypeRead: Read[SSLType] =
    Read[String].map(parseOrThrow(SSLType.fromString))

  implicit val sslTypeWrite: Write[SSLType] =
    Write[String].contramap(_.name)

  implicit val mimeTypeMeta: Meta[MimeType] =
    Meta[String].imap(parseOrThrow(MimeType.parse))(_.asString)

  implicit val mimeTypeRead: Read[MimeType] =
    Read[String].map(parseOrThrow(MimeType.parse))

  implicit val mimeTypeWrite: Write[MimeType] =
    Write[String].contramap(_.asString)

  implicit val mailAddressMeta: Meta[MailAddress] =
    Meta[String].imap(parseOrThrow(MailAddress.parse))(_.asUnicodeString)

  implicit val mailAddressRead: Read[MailAddress] =
    Read[String].map(parseOrThrow(MailAddress.parse))

  implicit val mailAddressWrite: Write[MailAddress] =
    Write[String].contramap(_.asUnicodeString)

  implicit val mailAddressListMeta: Meta[List[MailAddress]] =
    Meta[String].imap(parseOrThrow(MailAddress.parseMultiple))(
      _.map(_.asUnicodeString).mkString(",")
    )

  implicit val mailAddressListRead: Read[List[MailAddress]] =
    Read[String].map(parseOrThrow(MailAddress.parseMultiple))

  implicit val mailAddressListWrite: Write[List[MailAddress]] =
    Write[String].contramap(_.map(_.asUnicodeString).mkString(","))

  implicit def completeMailMeta[F[_]: Effect]: Meta[Mail[F]] =
    Meta[String].imap(str => Effect[F].toIO(Mail.deserialize(str)).unsafeRunSync())(m =>
      Effect[F].toIO(m.serialize).unsafeRunSync()
    )

  implicit def completeMailRead[F[_]: Effect]: Read[Mail[F]] =
    Read[String].map(str => Effect[F].toIO(Mail.deserialize(str)).unsafeRunSync())

  implicit def completeMailWrite[F[_]: Effect]: Write[Mail[F]] =
    Write[String].contramap(m => Effect[F].toIO(m.serialize).unsafeRunSync())
}

object EmilDoobieMeta extends EmilDoobieMeta {

  private def parseOrThrow[A](
      f: String => Either[String, A]
  )(str: String)(implicit ev: TypeName[A]): A =
    f(str) match {
      case Right(a)  => a
      case Left(err) => throw InvalidValue[String, A](str, err)
    }
}
