package emil.javamail.internal.ops

import cats.effect.Sync
import cats.syntax.all._
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
    FindMail[F](mh).andThen { optMime =>
      logger.debug(s"Loading complete raw mail for '$mh' from mime message '$optMime'")
      optMime.traverse(mime => Sync[F].delay(cm.convert(mime)))
    }

  def byUid[F[_]: Sync](folder: MailFolder, uid: MailUid)(implicit
      cm: Conv[MimeMessage, MailHeader],
      cb: Conv[MimeMessage, ByteVector]
  ): MailOp[F, JavaMailImapConnection, Map[MailHeader, ByteVector]] =
    FindMail.byUid[F](folder, uid).andThen { optMime =>
      logger.debug(s"Loaded complete raw mail for '$uid' from mime message '$optMime'")
      optMime
        .traverse { mime =>
          (Sync[F].delay(cm.convert(mime)), Sync[F].delay(cb.convert(mime))).tupled
        }
        .map(_.toList.toMap)
    }

  def byUid[F[_]: Sync](folder: MailFolder, start: MailUid, end: MailUid)(implicit
      cm: Conv[MimeMessage, MailHeader],
      cb: Conv[MimeMessage, ByteVector]
  ): MailOp[F, JavaMailImapConnection, Map[MailHeader, ByteVector]] =
    FindMail.byUid[F](folder, start, end).andThen { mimes =>
      logger
        .debug(
          s"Loaded complete raw mail from '$start' to '$end' from mime messages: ${mimes.size}"
        )
      mimes
        .traverse { mime =>
          (Sync[F].delay(cm.convert(mime)), Sync[F].delay(cb.convert(mime))).tupled
        }
        .map(_.toMap)
    }

  def byUid[F[_]: Sync](folder: MailFolder, uids: Set[MailUid])(implicit
      cm: Conv[MimeMessage, MailHeader],
      cb: Conv[MimeMessage, ByteVector]
  ): MailOp[F, JavaMailImapConnection, Map[MailHeader, ByteVector]] =
    FindMail.byUid[F](folder, uids).andThen { mimes =>
      logger
        .debug(
          s"Loaded complete raw mail for '$uids' from mime messages: ${mimes.size}"
        )
      mimes
        .traverse { mime =>
          (Sync[F].delay(cm.convert(mime)), Sync[F].delay(cb.convert(mime))).tupled
        }
        .map(_.toMap)
    }

}
