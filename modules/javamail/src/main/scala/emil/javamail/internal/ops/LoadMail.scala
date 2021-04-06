package emil.javamail.internal.ops

import cats.effect.Sync
import emil._
import emil.javamail.conv.Conv
import emil.javamail.internal.{JavaMailConnection, Logger}
import jakarta.mail.internet.MimeMessage

object LoadMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mh: MailHeader
  )(implicit
      cm: Conv[MimeMessage, Mail[F]]
  ): MailOp[F, JavaMailConnection, Option[Mail[F]]] =
    FindMail[F](mh).map { optMime =>
      logger.debug(s"Loading complete mail for '$mh' from mime message '$optMime'")
      optMime.map(cm.convert)
    }
}
