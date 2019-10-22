package emil.builder

import cats.Applicative
import emil._

class MailBuilder[F[_]](parts: Vector[Trans[F]], initial: Mail[F]) {

  def add(p0: Trans[F], ps: Trans[F]*): MailBuilder[F] =
    new MailBuilder[F]((parts :+ p0) ++ ps.toVector, initial)

  def set(p0: Trans[F], ps: Trans[F]*): MailBuilder[F] =
    add(p0, ps: _*)

  def prepend(p: Trans[F]): MailBuilder[F] =
    new MailBuilder[F](p +: parts, initial)

  def clearRecipients: MailBuilder[F] =
    prepend(Trans(m => m.mapMailHeader(_.mapRecipients(_ => Recipients.empty))))

  def clearAttachments: MailBuilder[F] =
    prepend(Trans(m => m.copy(attachments = Attachments.empty)))

  def clearBody(implicit F: Applicative[F]): MailBuilder[F] =
    prepend(Trans(m => m.mapBody(_ => MailBody.text(""))))

  def build: Mail[F] =
    parts.foldLeft(initial)((m, p) => p(m))
}

object MailBuilder {

  def apply[F[_]: Applicative](parts: Trans[F]*): MailBuilder[F] =
    new MailBuilder[F](parts.toVector, Mail.empty[F])

  def build[F[_]: Applicative](parts: Trans[F]*): Mail[F] =
    apply(parts: _*).build
}
