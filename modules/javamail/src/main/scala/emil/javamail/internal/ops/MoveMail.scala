package emil.javamail.internal.ops

import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits._
import com.sun.mail.imap.IMAPFolder
import emil._
import emil.javamail.internal.{JavaMailConnection, Logger, Util}
import jakarta.mail._
import jakarta.mail.internet.MimeMessage

object MoveMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mh: MailHeader,
      target: MailFolder
  ): MailOp[F, JavaMailConnection, Unit] =
    FindMail(mh).flatMap {
      case Some(msg) =>
        msg.getFolder match {
          case imf: IMAPFolder =>
            lift(logger.debugF(s"Move '$mh' via IMAP to '$target'.")) *>
              MailOp(conn =>
                Sync[F].blocking(moveNative(imf, msg, expectTargetFolder(conn, target)))
              )
          case f if f != null =>
            lift(logger.debugF(s"Move '$mh' via Copy to '$target'")) *>
              MailOp(conn =>
                Sync[F].blocking(moveViaCopy(f, msg, expectTargetFolder(conn, target)))
              )
          case _ =>
            lift(
              logger.debugF(s"Append '$mh' to folder '$target', no source folder found.")
            ) *>
              MailOp.of(conn =>
                expectTargetFolder(conn, target).appendMessages(Array(msg))
              )
        }
      case None =>
        MailOp.error("Mail to move not found.")
    }

  private def lift[F[_]](fa: F[Unit]): MailOp[F, JavaMailConnection, Unit] =
    Kleisli.liftF(fa)

  private[ops] def expectTargetFolder(
      conn: JavaMailConnection,
      target: MailFolder
  ): Folder = {
    val folder = conn.store.getFolder(target.id)
    if (folder == null || !folder.exists()) {
      logger.error(s"Target folder expected, but not found: $target")
      sys.error(s"Target folder '${target.name}' doesn't exist. Cannot move mail.")
    }
    folder
  }

  private def moveNative(source: IMAPFolder, msg: MimeMessage, target: Folder): Unit =
    try Util.withWriteFolder(source) { _ =>
      logger.trace(s"Move mail using imap protocol to '$target'.")
      source.moveMessages(Array[Message](msg), target)
    } catch {
      case ex: MessagingException =>
        logger.warn(
          s"Moving via imap protocol failed (${ex.getMessage}). Trying via copy."
        )
        moveViaCopy(source, msg, target)
    }

  private def moveViaCopy(source: Folder, msg: MimeMessage, target: Folder): Unit =
    Util.withWriteFolder(source) { _ =>
      logger.trace(s"Move via COPY to '$target'")
      source.copyMessages(Array(msg), target)
      source.setFlags(Array[Message](msg), new Flags(Flags.Flag.DELETED), true)
      source.expunge()
      ()
    }
}
