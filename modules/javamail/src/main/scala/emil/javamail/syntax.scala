package emil.javamail

import java.net.URL
import java.nio.file.Path

import fs2.io.file.Files
import cats.data.ValidatedNec
import cats.effect._
import cats.implicits._
import emil._
import emil.javamail.conv.MimeTypeDecode
import emil.javamail.conv.codec._
import fs2.{Pipe, Stream}
import jakarta.activation.MimeTypeParseException
import jakarta.mail.internet.AddressException
import scodec.bits.ByteVector

object syntax {

  implicit final class MailOps[F[_]: Sync](mail: Mail[F]) {
    def serialize: F[String] =
      JavaMailEmil.mailToString(mail)

    def toByteStream: Stream[F, Byte] =
      JavaMailEmil.mailToByteStream(mail)

    def toByteVector: F[ByteVector] =
      JavaMailEmil.mailToByteVector(mail)
  }

  implicit final class MailTypeOps(mt: Mail.type) {
    def deserialize[F[_]: Sync](str: String): F[Mail[F]] =
      JavaMailEmil.mailFromString(str)

    def deserializeByteArray[F[_]: Sync](ba: Array[Byte]): F[Mail[F]] =
      JavaMailEmil.mailFromByteArray(ba)

    def deserializeByteVector[F[_]: Sync](bv: ByteVector): F[Mail[F]] =
      JavaMailEmil.mailFromByteVector(bv)

    def fromFile[F[_]: Files: Sync](file: Path): F[Mail[F]] =
      Files[F]
        .readAll(file, 8192)
        .through(readBytes[F])
        .compile
        .lastOrError

    def fromURL[F[_]: Sync](url: URL): F[Mail[F]] =
      fs2.io
        .readInputStream(Sync[F].blocking(url.openStream), 8192, true)
        .through(readBytes[F])
        .compile
        .lastOrError

    def readBytes[F[_]: Sync]: Pipe[F, Byte, Mail[F]] =
      _.chunks
        .map(_.toByteVector)
        .fold(ByteVector.empty)(_ ++ _)
        .evalMap(deserializeByteVector[F])
  }

  implicit final class MimeTypeTypeOps(mt: MimeType.type) {
    def parse(str: String): Either[String, MimeType] =
      MimeTypeDecode.parse(str)

    def parseValidated(str: String): ValidatedNec[MimeTypeParseException, MimeType] =
      MimeTypeDecode.parseValidated(str)

    def parseUnsafe(str: String): MimeType =
      parse(str).fold(sys.error, identity)
  }

  implicit final class MailAddressTypeOps(mat: MailAddress.type) {
    def parse(str: String): Either[String, MailAddress] =
      mailAddressParse.convert(str)

    /** Parses an email address returning a cats `ValidatedNec` with `AddressException` as the error in case of malformed address. */
    def parseValidated(str: String): ValidatedNec[AddressException, MailAddress] =
      mailAddressParseValidated.convert(str)

    /** Parses an email address from two parts. */
    def parseAddressAndName(
        name: Option[String],
        address: String
    ): Either[String, MailAddress] =
      mailAddressParseNameAndAddress.convert((name, address))

    /** Parses an email address from two parts returning a cats `ValidatedNec` with `AddressException` as the error in case of malformed address. */
    def parseAddressAndNameValidated(
        name: Option[String],
        address: String
    ): ValidatedNec[AddressException, MailAddress] =
      mailAddressParseNameAndAddressValidated.convert((name, address))

    def parseUnsafe(str: String): MailAddress =
      parseValidated(str).fold(nec => throw nec.head, identity)

    /** Reads a comma-separated list of e-mail addresses.
      */
    def parseMultiple(str: String): Either[String, List[MailAddress]] =
      str.split(Array(',', ';')).toList.map(_.trim).traverse(parse)

    /** Reads a comma-separated list of e-mail addresses,
      *  returning a cats `ValidatedNec` with `AddressException` as the error in case of malformed addresses.
      */
    def parseMultipleValidated(
        str: String
    ): ValidatedNec[AddressException, List[MailAddress]] =
      str.split(Array(',', ';')).toList.map(_.trim).traverse(parseValidated)

    /** Reads a comma-separated list of e-mail addresses, throwing
      * exceptions if something fails.
      */
    def parseMultipleUnsafe(str: String): List[MailAddress] =
      parseMultiple(str).fold(sys.error, identity)
  }

  implicit final class MailAddressOps(ma: MailAddress) {
    def asAsciiString: String =
      mailAddressEncode.convert(ma).toString

    def asUnicodeString: String =
      mailAddressEncode.convert(ma).toUnicodeString
  }
}
