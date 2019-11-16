package emil.javamail.internal.ops

import cats.effect.Sync
import cats.implicits._
import emil._
import emil.javamail.conv.{MessageIdEncode, MsgConv}
import emil.javamail.internal.{JavaMailConnection, Logger, ThreadClassLoader}
import javax.mail.internet.MimeMessage

object SendMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mails: Seq[Mail[F]]
  )(implicit cm: MsgConv[Mail[F], F[MimeMessage]]): MailOp[F, JavaMailConnection, Unit] =
    MailOp(
      conn =>
        conn
          .transport[F]
          .use(trans => {

            mails.toList.traverse(
              mail =>
                cm.convert(conn.session, MessageIdEncode.Random, mail)
                  .flatMap({ msg =>
                    Sync[F].delay({
                      logger.debug(s"Sending message: ${infoLine(mail.header)}")
                      ThreadClassLoader {
                        // this can be required if the mailcap file is not found
                        trans.sendMessage(msg, msg.getAllRecipients)
                      }
                    })
                  })
            )

          })
          .map(_ => ())
    )

  private def infoLine(mh: MailHeader): String =
    s"${mh.subject}:${mh.from.map(_.address).getOrElse("<no-from>")}->${mh.recipients.to.map(_.address)}"

}
