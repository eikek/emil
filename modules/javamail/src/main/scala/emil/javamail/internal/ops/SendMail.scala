package emil.javamail.internal.ops

import cats.effect.Sync
import cats.implicits._
import cats.data.NonEmptyList
import emil._
import emil.javamail.conv.{MessageIdEncode, MsgConv}
import emil.javamail.internal.{JavaMailConnection, Logger, ThreadClassLoader}
import javax.mail.internet.MimeMessage
import javax.mail.Transport

object SendMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mails: NonEmptyList[Mail[F]]
  )(
      implicit cm: MsgConv[Mail[F], F[MimeMessage]]
  ): MailOp[F, JavaMailConnection, NonEmptyList[String]] =
    MailOp(conn =>
      mails.traverse(mail =>
        cm.convert(conn.session, MessageIdEncode.Random, mail)
          .flatMap({ msg =>
            Sync[F].delay({
              val msgId = checkMessageID(msg)
              logger.debug(s"Sending message: ${infoLine(mail.header)}, $msgId")
              // this can be required if the mailcap file is not found
              ThreadClassLoader {
                if (conn.config.user.nonEmpty) {
                  Transport
                    .send(msg, msg.getAllRecipients, conn.config.user, conn.config.password)
                } else {
                  Transport.send(msg, msg.getAllRecipients)
                }
              }
              msgId
            })
          })
      )
    )

  private def checkMessageID(msg: MimeMessage): String =
    Option(msg.getMessageID) match {
      case Some(id) => id
      case None     => sys.error("No messageID found for mail being prepared for sending.")
    }

  private def infoLine(mh: MailHeader): String =
    s"${mh.subject}:${mh.from.map(_.address).getOrElse("<no-from>")}->${mh.recipients.to.map(_.address)}"

}
