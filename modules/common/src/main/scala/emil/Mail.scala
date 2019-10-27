package emil

import cats.Applicative
import emil.builder.MailBuilder

final case class Mail[F[_]]( header: MailHeader
                     , additionalHeaders: Headers
                     , body: MailBody[F]
                     , attachments: Attachments[F]
                     ) {

  def mapMailHeader(f: MailHeader => MailHeader): Mail[F] =
    copy(header = f(header))

  def mapHeaders(f: Headers => Headers): Mail[F] =
    copy(additionalHeaders = f(additionalHeaders))

  def mapBody(f: MailBody[F] => MailBody[F]): Mail[F] =
    copy(body = f(body))

  def mapAttachments(f: Attachments[F] => Attachments[F]): Mail[F] =
    copy(attachments = f(attachments))

  def asBuilder: MailBuilder[F] =
    new MailBuilder[F](Vector.empty, this)
}

object Mail {

  def empty[F[_]: Applicative]: Mail[F] =
    Mail(MailHeader.empty, Headers.empty, MailBody.empty[F], Attachments.empty[F])

}