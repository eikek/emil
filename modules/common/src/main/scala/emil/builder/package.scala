package emil

import fs2.Stream
import fs2.io.file.{readAll => readFile}
import fs2.io.readInputStream
import cats.implicits._

package builder {

  import java.io.InputStream
  import java.net.URL
  import java.nio.file.{Files, Path}

  import cats.Applicative
  import cats.data.NonEmptyList
  import cats.effect.{Blocker, ContextShift, Sync}
  import emil.MimeType

  trait Trans[F[_]] {
    def apply(mail: Mail[F]): Mail[F]
  }
  object Trans {
    def apply[F[_]](f: Mail[F] => Mail[F]): Trans[F] =
      (m: Mail[F]) => f(m)
  }

  case class From[F[_]](ma: MailAddress) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.copy(from = Some(ma)))
  }

  object From extends MailAddressHelper[From]

  case class To[F[_]](ma: MailAddress) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.mapRecipients(_.addTo(ma)))
  }
  object To extends MailAddressHelper[To]

  case class Cc[F[_]](ma: MailAddress) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.mapRecipients(_.addCc(ma)))
  }
  object Cc extends MailAddressHelper[Cc]

  case class Bcc[F[_]](ma: MailAddress) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.mapRecipients(_.addBcc(ma)))
  }
  object Bcc extends MailAddressHelper[Bcc]

  case class Subject[F[_]](text: String) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.withSubject(text))
  }

  case class TextBody[F[_]](text: F[String]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapBody(_.withText(text))
  }
  object TextBody {
    def apply[F[_]: Applicative](text: String): TextBody[F] =
      TextBody(text.pure[F])
  }

  case class HtmlBody[F[_]](html: F[String]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapBody(_.withHtml(html))
  }
  object HtmlBody {
    def apply[F[_]: Applicative](html: String): HtmlBody[F] =
      HtmlBody(html.pure[F])
  }

  case class CustomHeader[F[_]](header: Header) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapHeaders(_.add(header))
  }
  object CustomHeader {
    def apply[F[_]](name: String, value: String, more: String*): CustomHeader[F] =
      CustomHeader[F](Header(name, NonEmptyList.of(value, more: _*)))
  }

  object MessageID {
    def apply[F[_]](id: String): Trans[F] =
      Trans(mail => mail.mapMailHeader(_.withMessageID(id)))
  }

  case class Attach[F[_]](attach: Attachment[F]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapAttachments(_.add(attach))

    def withMimeType(mimeType: MimeType): Attach[F] =
      copy(attach = attach.copy(mimeType = mimeType))

    def withFilename(name: String): Attach[F] =
      copy(attach = attach.copy(filename = Some(name)))

    def withLength(len: Long)(implicit ev: Applicative[F]): Attach[F] =
      copy(attach = attach.copy(length = len.pure[F]))

    def withLength(len: F[Long]): Attach[F] =
      copy(attach = attach.copy(length = len))
  }

  object Attach {
    def apply[F[_]: Sync](data: Stream[F, Byte]): Attach[F] =
      Attach(Attachment(None, MimeType.octetStream, data))
  }

  object AttachStream {
    def apply[F[_]: Sync](
        data: Stream[F, Byte],
        filename: Option[String] = None,
        mimeType: MimeType = MimeType.octetStream
    ): Attach[F] =
      Attach(Attachment(filename, mimeType, data))
  }

  case class AttachFile[F[_]: Sync: ContextShift](
      file: Path,
      blocker: Blocker,
      mimeType: MimeType = MimeType.octetStream,
      filename: Option[String] = None,
      chunkSize: Int = 8 * 1024
  ) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      Attach(
        Attachment(
          filename.orElse(Some(file.getFileName.toString)),
          mimeType,
          readFile(file, blocker, chunkSize),
          Sync[F].delay(Files.size(file))
        )
      ).apply(mail)

    def withFilename(name: String): AttachFile[F] =
      copy(filename = Some(name))

    def withMimeType(mimeType: MimeType): AttachFile[F] =
      copy(mimeType = mimeType)

    def withChunkSize(size: Int): AttachFile[F] =
      copy(chunkSize = size)
  }

  case class AttachInputStream[F[_]: Sync: ContextShift](
      is: F[InputStream],
      blocker: Blocker,
      filename: Option[String] = None,
      mimeType: MimeType = MimeType.octetStream,
      length: Option[F[Long]] = None,
      chunkSize: Int = 8 * 1024
  ) extends Trans[F] {

    def apply(mail: Mail[F]): Mail[F] = {
      val data = readInputStream(is, chunkSize, blocker)
      Attach(
        length
          .map(len => Attachment(filename, mimeType, data, len))
          .getOrElse(Attachment(filename, mimeType, data))
      ).apply(mail)
    }

    def withFilename(name: String): AttachInputStream[F] =
      copy(filename = Some(name))

    def withMimeType(mimeType: MimeType): AttachInputStream[F] =
      copy(mimeType = mimeType)

    def withLength(len: Long): AttachInputStream[F] =
      copy(length = len.pure[F].some)

    def withLength(len: F[Long]): AttachInputStream[F] =
      copy(length = len.some)

    def withChunkSize(size: Int): AttachInputStream[F] =
      copy(chunkSize = size)
  }

  object AttachUrl {
    def apply[F[_]: Sync: ContextShift](
        url: URL,
        blocker: Blocker,
        filename: Option[String] = None,
        mimeType: MimeType = MimeType.octetStream,
        length: Option[F[Long]] = None,
        chunkSize: Int = 16 * 1024
    ): AttachInputStream[F] = {
      require(url != null, "Url must not be null")
      AttachInputStream(
        Sync[F].delay(url.openStream()),
        blocker,
        filename,
        mimeType,
        length,
        chunkSize
      )
    }
  }

  trait MailAddressHelper[A[_[_]]] {
    def apply[F[_]](ma: MailAddress): A[F]

    def apply[F[_]](address: String): A[F] =
      apply(MailAddress.unsafe(None, address))

    def apply[F[_]](personal: String, address: String): A[F] =
      apply(MailAddress.unsafe(Some(personal), address))
  }
}
