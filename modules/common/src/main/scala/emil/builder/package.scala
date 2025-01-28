package emil

import cats.implicits._
import fs2.Stream
import fs2.io.file.{Files, Flags, Path}
import fs2.io.readInputStream

package builder {

  import java.io.InputStream
  import java.net.URL

  import cats.Applicative
  import cats.data.NonEmptyList
  import cats.effect._
  import emil.MimeType
  import java.time.Instant
  import java.time.temporal.ChronoField

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

  case class Tos[F[_]](mas: Seq[MailAddress]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.mapRecipients(_.addTos(mas)))
  }

  case class Cc[F[_]](ma: MailAddress) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.mapRecipients(_.addCc(ma)))
  }
  object Cc extends MailAddressHelper[Cc]

  case class Ccs[F[_]](mas: Seq[MailAddress]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.mapRecipients(_.addCcs(mas)))
  }

  case class Bcc[F[_]](ma: MailAddress) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.mapRecipients(_.addBcc(ma)))
  }
  object Bcc extends MailAddressHelper[Bcc]

  case class Bccs[F[_]](mas: Seq[MailAddress]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.mapRecipients(_.addBccs(mas)))
  }

  case class ReplyTo[F[_]](ma: MailAddress) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.copy(replyTo = Some(ma)))
  }

  object ReplyTo extends MailAddressHelper[ReplyTo]

  case class Subject[F[_]](text: String) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(_.withSubject(text))
  }

  case class Date[F[_]](date: Instant) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapMailHeader(
        _.withOriginationDate(date.`with`(ChronoField.MILLI_OF_SECOND, 0))
      )
  }

  case class TextBody[F[_]](text: F[BodyContent]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapBody(_.withText(text))
  }
  object TextBody {
    def apply[F[_]: Applicative](text: BodyContent): TextBody[F] =
      TextBody(text.pure[F])
    def apply[F[_]: Applicative](text: String): TextBody[F] =
      apply(BodyContent(text))
  }

  case class HtmlBody[F[_]](html: F[BodyContent]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapBody(_.withHtml(html))
  }
  object HtmlBody {
    def apply[F[_]: Applicative](html: BodyContent): HtmlBody[F] =
      HtmlBody(html.pure[F])
    def apply[F[_]: Applicative](html: String): HtmlBody[F] =
      HtmlBody(BodyContent(html))
  }

  case class CustomHeader[F[_]](header: Header) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      if (header.isEmpty) mail
      else mail.mapHeaders(_.add(header))

    /** By default, empty headers are ignored and not set into the mail. This allows to
      * set headers to an empty value.
      */
    def allowEmpty: Trans[F] =
      Trans(_.mapHeaders(_.add(header)))
  }
  object CustomHeader {
    def apply[F[_]](name: String, value: String, more: String*): CustomHeader[F] =
      CustomHeader[F](Header(name, NonEmptyList(value, more.toList)))

    def apply[F[_]](name: String, value: Option[String]): Trans[F] =
      value.map(v => apply[F](name, v)).getOrElse(Trans.id[F])
  }

  object MessageID extends CustomHeaderOption {
    def apply[F[_]](id: String): Trans[F] =
      if (id.isEmpty) Trans.id[F]
      else Trans(mail => mail.mapMailHeader(_.withMessageID(id)))
  }

  object UserAgent extends CustomHeaderOption {
    def apply[F[_]](value: String): Trans[F] =
      CustomHeader[F](Header.userAgent(value))

    def emil[F[_]]: Trans[F] =
      apply(s"Emil/${BuildInfo.version}")
  }
  object XMailer extends CustomHeaderOption {
    def apply[F[_]](value: String): Trans[F] =
      CustomHeader[F](Header.xmailer(value))

    def emil[F[_]]: Trans[F] =
      apply(s"Emil/${BuildInfo.version}")
  }
  object ListId extends CustomHeaderOption {
    def apply[F[_]](value: String): Trans[F] =
      CustomHeader[F](Header.listId(value))
  }
  object InReplyTo {
    def apply[F[_]](value: String, more: String*): Trans[F] =
      CustomHeader[F](Header.inReplyTo(NonEmptyList(value, more.toList)))
  }

  case class Attach[F[_]](attach: Attachment[F]) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      mail.mapAttachments(_.add(attach))

    def withMimeType(mimeType: MimeType): Attach[F] =
      copy(attach = attach.copy(mimeType = mimeType))

    def withMimeType(mimeType: Option[MimeType]): Attach[F] =
      copy(attach = attach.copy(mimeType = mimeType.getOrElse(MimeType.octetStream)))

    def withFilename(name: String): Attach[F] =
      copy(attach = attach.copy(filename = Some(name)))

    def withFilename(name: Option[String]): Attach[F] =
      copy(attach = attach.copy(filename = name))

    def withLength(len: Long)(implicit ev: Applicative[F]): Attach[F] =
      copy(attach = attach.copy(length = len.pure[F]))

    def withLength(len: F[Long]): Attach[F] =
      copy(attach = attach.copy(length = len))

    def withDisposition(disp: Disposition): Attach[F] =
      copy(attach = attach.copy(disposition = Some(disp)))

    def withDisposition(disp: Option[Disposition]): Attach[F] =
      copy(attach = attach.copy(disposition = disp))

    def withContentId(cid: String): Attach[F] =
      copy(attach = attach.copy(contentId = Some(cid)))

    def withContentId(cid: Option[String]): Attach[F] =
      copy(attach = attach.copy(contentId = cid))

    def withInlinedContentId(cid: String): Attach[F] =
      copy(attach =
        attach.copy(contentId = Some(cid), disposition = Some(Disposition.Inline))
      )

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

  case class AttachFile[F[_]: Files](
      file: Path,
      mimeType: MimeType = MimeType.octetStream,
      filename: Option[String] = None,
      chunkSize: Int = 64 * 1024
  ) extends Trans[F] {
    def apply(mail: Mail[F]): Mail[F] =
      Attach(
        Attachment(
          filename.orElse(Some(file.fileName.toString)),
          mimeType,
          Files[F].readAll(file, chunkSize, Flags.Read),
          Files[F].size(file)
        )
      ).apply(mail)

    def withFilename(name: String): AttachFile[F] =
      copy(filename = Some(name))

    def withMimeType(mimeType: MimeType): AttachFile[F] =
      copy(mimeType = mimeType)

    def withChunkSize(size: Int): AttachFile[F] =
      copy(chunkSize = size)
  }

  case class AttachInputStream[F[_]: Async](
      is: F[InputStream],
      filename: Option[String] = None,
      mimeType: MimeType = MimeType.octetStream,
      length: Option[F[Long]] = None,
      chunkSize: Int = 8 * 1024,
      disposition: Option[Disposition] = None,
      contentId: Option[String] = None
  ) extends Trans[F] {

    def apply(mail: Mail[F]): Mail[F] = {
      val data = readInputStream(is, chunkSize)
      Attach(
        length
          .map(len => Attachment(filename, mimeType, data, len, disposition, contentId))
          .getOrElse(Attachment(filename, mimeType, data, disposition, contentId))
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

    def withDisposition(disp: Disposition): AttachInputStream[F] =
      copy(disposition = Some(disp))

    def withContentId(cid: String): AttachInputStream[F] =
      copy(contentId = Some(cid))

    def withInlinedContentId(cid: String): AttachInputStream[F] =
      copy(contentId = Some(cid), disposition = Some(Disposition.Inline))
  }

  object AttachUrl {
    def apply[F[_]: Async](
        url: URL,
        filename: Option[String] = None,
        mimeType: MimeType = MimeType.octetStream,
        length: Option[F[Long]] = None,
        chunkSize: Int = 16 * 1024
    ): AttachInputStream[F] = {
      require(url != null, "Url must not be null")
      AttachInputStream(
        Sync[F].blocking(url.openStream()),
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

  trait CustomHeaderOption {
    def apply[F[_]](value: String): Trans[F]

    def apply[F[_]](value: Option[String]): Trans[F] =
      value.map(apply[F]).getOrElse(Trans.id[F])
  }
}
