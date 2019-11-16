package emil.javamail.internal.ops

import cats.effect._
import emil.javamail.internal.{JavaMailConnection, Logger}
import emil._
import emil.javamail.conv.{MessageIdEncode, MsgConv}
import javax.mail.Folder
import javax.mail.internet.MimeMessage

object PutMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](mail: Mail[F], target: MailFolder)(
      implicit cm: MsgConv[Mail[F], F[MimeMessage]]
  ): MailOp[F, JavaMailConnection, Unit] =
    for {
      folder <- mailOp(conn => MoveMail.expectTargetFolder(conn, target))
      msg <- mailOpF(conn => cm.convert(conn.session, MessageIdEncode.GivenOrRandom, mail))
      _ <- mailOpF(_ => logger.debugF(s"Append mail ${mail.header.id} to folder ${target.id}"))
      _ = appendMessage(msg, folder)
    } yield ()

  private def appendMessage(msg: MimeMessage, folder: Folder): Unit =
    folder.appendMessages(Array(msg))

  private def mailOpF[F[_], A](f: JavaMailConnection => F[A]): MailOp[F, JavaMailConnection, A] =
    MailOp(f)

  private def mailOp[F[_]: Sync, A](f: JavaMailConnection => A): MailOp[F, JavaMailConnection, A] =
    MailOp.of[F, JavaMailConnection, A](f)
}
