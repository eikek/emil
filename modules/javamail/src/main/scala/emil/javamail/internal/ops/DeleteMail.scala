package emil.javamail.internal.ops

import cats.effect.Sync
import cats.implicits._
import emil._
import emil.javamail.internal.{JavaMailConnection, Logger, Util}
import jakarta.mail.internet.MimeMessage
import jakarta.mail.{Flags, Folder, Message}

object DeleteMail {
  private val logger = Logger(getClass)

  def apply[F[_]: Sync](mh: MailHeader): MailOp[F, JavaMailConnection, Int] =
    FindMail(mh).andThen(opt =>
      opt match {
        case Some(msg) => delete(msg, mh)
        case _ =>
          logger.infoF(s"Cannot delete message '$mh', it was not found.") *> 0.pure[F]
      }
    )

  private def delete[F[_]: Sync](msg: MimeMessage, mh: MailHeader): F[Int] =
    Sync[F].blocking {
      msg.getFolder match {
        case f: Folder =>
          Util.withWriteFolder(f) { f =>
            logger.debug(s"Delete message '$mh' now.")
            f.setFlags(
              Array(msg.asInstanceOf[Message]),
              new Flags(Flags.Flag.DELETED),
              true
            )
          }
          1
        case null =>
          logger.warn(s"Not deleting message. No folder available.")
          0
      }
    }
}
