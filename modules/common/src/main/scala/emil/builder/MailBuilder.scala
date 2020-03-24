package emil.builder

import cats.Applicative
import emil._

final class MailBuilder[F[_]](parts: Vector[Trans[F]], initial: Mail[F]) {

  def add(p0: Trans[F], ps: Trans[F]*): MailBuilder[F] =
    new MailBuilder[F]((parts :+ p0) ++ ps.toVector, initial)

  def addAll(ps: Seq[Trans[F]]): MailBuilder[F] =
    new MailBuilder(parts ++ ps.toVector, initial)

  def set(p0: Trans[F], ps: Trans[F]*): MailBuilder[F] =
    add(p0, ps: _*)

  def prepend(p: Trans[F]): MailBuilder[F] =
    new MailBuilder[F](p +: parts, initial)

  /** Prepends an action that clears all recipients.
    */
  def clearRecipients: MailBuilder[F] =
    prepend(Trans(m => m.mapMailHeader(_.mapRecipients(_ => Recipients.empty))))

  /** Prepends an action that clears all attachments.
    */
  def clearAttachments: MailBuilder[F] =
    prepend(Trans(m => m.copy(attachments = Attachments.empty)))

  /** Prepends an action that clears the body.
    */
  def clearBody: MailBuilder[F] =
    prepend(Trans(m => m.mapBody(_ => MailBody.Empty[F])))

  def build: Mail[F] =
    parts.foldLeft(initial)((m, p) => p(m))
}

object MailBuilder {

  def fromSeq[F[_]: Applicative](parts: Seq[Trans[F]]): MailBuilder[F] =
    new MailBuilder[F](parts.toVector, Mail.empty[F])

  def apply[F[_]: Applicative](parts: Trans[F]*): MailBuilder[F] =
    new MailBuilder[F](parts.toVector, Mail.empty[F])

  def build[F[_]: Applicative](parts: Trans[F]*): Mail[F] =
    apply(parts: _*).build
}
