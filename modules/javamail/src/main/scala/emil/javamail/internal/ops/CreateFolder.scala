package emil.javamail.internal.ops

import cats.effect.Sync
import emil._
import emil.javamail.conv.Conv
import emil.javamail.internal.{JavaMailConnection, Logger}
import javax.mail.Folder

object CreateFolder {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](parent: Option[MailFolder], name: String)(
      implicit c: Conv[Folder, MailFolder]
  ): MailOp[F, JavaMailConnection, MailFolder] =
    MailOp(conn =>
      Sync[F].delay {
        val f = parent
          .map(p => conn.store.getFolder(p.id).getFolder(name))
          .getOrElse(conn.store.getFolder(name))
        if (!f.exists()) {
          logger.debug(s"Creating new folder '$name' at '${conn.config}'")
          f.create(Folder.HOLDS_MESSAGES)
        }
        c.convert(f)
      }
    )
}
