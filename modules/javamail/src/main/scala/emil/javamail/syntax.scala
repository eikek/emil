package emil.javamail

import cats.implicits._
import cats.effect.Sync
import emil._
import emil.javamail.conv.MimeTypeDecode
import emil.javamail.conv.codec._

object syntax {

  implicit final class MailOps[F[_]: Sync](mail: Mail[F]) {
    def serialize: F[String] =
      JavaMailEmil.mailToString[F](mail)
  }

  implicit final class MailTypeOps(mt: Mail.type) {
    def deserialize[F[_]: Sync](str: String): F[Mail[F]] =
      JavaMailEmil.mailFromString(str)
  }

  implicit final class MimeTypeTypeOps(mt: MimeType.type) {
    def parse(str: String): Either[String, MimeType] =
      MimeTypeDecode.parse(str)
  }

  implicit final class MailAddressTypeOps(mat: MailAddress.type) {
    def parse(str: String): Either[String, MailAddress] =
      Either.catchNonFatal(mailAddressParse.convert(str)).leftMap(_.getMessage)
  }

  implicit final class MailAddressOps(ma: MailAddress) {
    def asAsciiString: String =
      mailAddressEncode.convert(ma).toString

    def asUnicodeString: String =
      mailAddressEncode.convert(ma).toUnicodeString
  }
}
