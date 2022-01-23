package emil.javamail.internal.ops

import cats.effect.Sync
import emil._
import emil.javamail.conv.Conv
import emil.javamail.internal.{JavaMailConnection, JavaMailImapConnection, Logger}
import jakarta.mail.internet.MimeMessage
import scodec.bits.ByteVector

object LoadMailRaw {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mh: MailHeader
  )(implicit
      cm: Conv[MimeMessage, ByteVector]
  ): MailOp[F, JavaMailConnection, Option[ByteVector]] =
    FindMail[F](mh).map { optMime =>
      logger.debug(s"Loading complete raw mail for '$mh' from mime message '$optMime'")
      optMime.map(cm.convert)
    }

  def byUid[F[_]: Sync](folder: MailFolder, uid: MailUid)(implicit
      cm: Conv[MimeMessage, ByteVector]
  ): MailOp[F, JavaMailImapConnection, Option[ByteVector]] =
    FindMail.byUid[F](folder, uid).map { optMime =>
      logger
        .debug(s"Loaded complete raw mail for '$uid' from mime message '$optMime'")
      optMime.map(cm.convert)
    }

  def byUid[F[_]: Sync](folder: MailFolder, start: MailUid, end: MailUid)(implicit
      cm: Conv[MimeMessage, ByteVector]
  ): MailOp[F, JavaMailImapConnection, List[ByteVector]] =
    FindMail.byUid[F](folder, start, end).map { optMime =>
      logger
        .debug(
          s"Loaded complete raw mail from '$start' to '$end' from mime messages '$optMime'"
        )
      optMime.map(cm.convert)
    }

  def byUid[F[_]: Sync](folder: MailFolder, uids: List[MailUid])(implicit
      cm: Conv[MimeMessage, ByteVector]
  ): MailOp[F, JavaMailImapConnection, List[ByteVector]] =
    FindMail.byUid[F](folder, uids).map { optMime =>
      logger
        .debug(
          s"Loaded complete raw mail for '$uids' from mime messages '$optMime'"
        )
      optMime.map(cm.convert)
    }

}
