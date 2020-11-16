package emil.javamail.internal.ops

import jakarta.mail.Folder
import jakarta.mail.internet.MimeMessage

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import emil._
import emil.javamail.internal.{JavaMailConnection, Logger, Util}

object CopyMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mh: MailHeader,
      target: MailFolder
  ): MailOp[F, JavaMailConnection, Unit] =
    FindMail(mh).flatMap {
      case Some(msg) =>
        msg.getFolder match {
          case f if f != null =>
            MailOp.of(conn => copy(f, msg, MoveMail.expectTargetFolder(conn, target)))
          case _ =>
            lift(
              logger.debugF(s"Append '$mh' to folder '$target', no soruce folder found.")
            ) *>
              MailOp.of(conn =>
                MoveMail.expectTargetFolder(conn, target).appendMessages(Array(msg))
              )
        }
      case None =>
        MailOp.error("Mail to copy not found.")
    }

  private def lift[F[_]](fa: F[Unit]): MailOp[F, JavaMailConnection, Unit] =
    Kleisli.liftF(fa)

  private def copy(source: Folder, msg: MimeMessage, target: Folder): Unit =
    Util.withWriteFolder(source) { _ =>
      logger.trace(s"Copy to '$target'")
      source.copyMessages(Array(msg), target)
      ()
    }

}
