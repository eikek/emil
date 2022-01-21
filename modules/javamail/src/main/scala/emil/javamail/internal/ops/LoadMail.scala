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

  def byUid[F[_]: Sync](folder: MailFolder, uid: MailUid)(implicit
      cm: Conv[MimeMessage, Mail[F]]
  ): MailOp[F, JavaMailConnection, Option[Mail[F]]] =
    FindMail.byUid[F](folder, uid).map { optMime =>
      logger
        .debug(s"Loaded complete raw mail for '$uid' from mime message '$optMime'")
      optMime.map(cm.convert)
    }

  def byUid[F[_]: Sync](folder: MailFolder, start: MailUid, end: MailUid)(implicit
      cm: Conv[MimeMessage, Mail[F]]
  ): MailOp[F, JavaMailConnection, List[Mail[F]]] =
    FindMail.byUid[F](folder, start, end).map { optMime =>
      logger
        .debug(
          s"Loaded complete raw mail from '$start' to '$end' from mime messages '$optMime'"
        )
      optMime.map(cm.convert)
    }

  def byUid[F[_]: Sync](folder: MailFolder, uids: List[MailUid])(implicit
      cm: Conv[MimeMessage, Mail[F]]
  ): MailOp[F, JavaMailConnection, List[Mail[F]]] =
    FindMail.byUid[F](folder, uids).map { optMime =>
      logger
        .debug(
          s"Loaded complete raw mail for '$uids' from mime messages '$optMime'"
        )
      optMime.map(cm.convert)
    }
}
