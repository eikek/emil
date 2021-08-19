package emil.javamail.internal.ops

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import emil._
import emil.javamail.conv.{MessageIdEncode, MsgConv}
import emil.javamail.internal.{JavaMailConnection, Logger, ThreadClassLoader}
import jakarta.mail.internet.MimeMessage

object SendMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mails: NonEmptyList[Mail[F]]
  )(implicit
      cm: MsgConv[Mail[F], F[MimeMessage]]
  ): MailOp[F, JavaMailConnection, NonEmptyList[String]] =
    MailOp(conn =>
      logger.debugF(s"Sending ${mails.size} mail(s) using ${conn.config}") *>
        mails.traverse { mail =>
          ThreadClassLoader {
            cm.convert(conn.session, MessageIdEncode.Random, mail).flatMap { msg =>
              val msgId = checkMessageID(msg)
              Sync[F].blocking {
                logger.debug(s"Sending message: ${infoLine(mail.header)}, $msgId")
                conn.transport.sendMessage(msg, msg.getAllRecipients)
                logger.debug("Mail sent")
                msgId
              }
            }
          }
        }
    )

  private def checkMessageID(msg: MimeMessage): String =
    Option(msg.getMessageID) match {
      case Some(id) => id
      case None => sys.error("No messageID found for mail being prepared for sending.")
    }

  private def infoLine(mh: MailHeader): String =
    s"${mh.subject}:${mh.from.map(_.address).getOrElse("<no-from>")}->${mh.recipients.to
      .map(_.address)}"

}
