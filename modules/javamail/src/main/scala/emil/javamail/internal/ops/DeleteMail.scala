package emil.javamail.internal.ops

import javax.mail.internet.MimeMessage
import javax.mail.{Flags, Folder, Message}

import cats.effect.Sync
import cats.implicits._
import emil._
import emil.javamail.internal.{JavaMailConnection, Logger, Util}

object DeleteMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](mh: MailHeader): MailOp[F, JavaMailConnection, Int] =
    FindMail(mh).andThen(opt =>
      opt match {
        case Some(msg) => delete(msg, mh)
        case _ =>
          logger.infoF(s"Cannot delete message '$mh', it was not found.") *> 0.pure[F]
      }
    )

  private def delete[F[_]: Sync](msg: MimeMessage, mh: MailHeader): F[Int] =
    Sync[F].delay {
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
        case _ =>
          logger.warn(s"Not deleting message. No folder available.")
          0
      }
    }
}
