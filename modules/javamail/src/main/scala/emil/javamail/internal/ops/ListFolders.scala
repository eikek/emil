package emil.javamail.internal.ops

import cats.effect.Sync
import emil._
import emil.javamail.conv.Conv
import emil.javamail.internal.JavaMailConnection
import jakarta.mail.Folder

object ListFolders {

  def apply[F[_]: Sync](parent: Option[MailFolder])(implicit
      c: Conv[Folder, MailFolder]
  ): MailOp[F, JavaMailConnection, Vector[MailFolder]] =
    MailOp(conn =>
      Sync[F].blocking {
        parent
          .map(pf => conn.store.getFolder(pf.id))
          .getOrElse(conn.store.getDefaultFolder)
          .list()
          .map(c.convert)
          .toVector
      }
    )

}
