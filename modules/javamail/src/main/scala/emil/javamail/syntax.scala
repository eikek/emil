package emil.javamail

import cats.implicits._
import cats.effect._
import emil._
import emil.javamail.conv.MimeTypeDecode
import emil.javamail.conv.codec._
import java.nio.file.Path

object syntax {

  implicit final class MailOps[F[_]: Sync](mail: Mail[F]) {
    def serialize: F[String] =
      JavaMailEmil.mailToString[F](mail)
  }

  implicit final class MailTypeOps(mt: Mail.type) {
    def deserialize[F[_]: Sync](str: String): F[Mail[F]] =
      JavaMailEmil.mailFromString(str)

    def fromFile[F[_]: Sync: ContextShift](file: Path, blocker: Blocker): F[Mail[F]] =
      fs2.io.file
        .readAll(file, blocker, 8192)
        .through(fs2.text.utf8Decode)
        .foldMonoid
        .evalMap(deserialize[F])
        .compile
        .lastOrError
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
