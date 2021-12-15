package emil.javamail.internal.ops

import cats.effect.Sync
import emil.javamail.conv.Conv
import emil.javamail.internal.JavaMailConnection
import emil.{MailFolder, MailOp}
import jakarta.mail.Folder

object ListFoldersRecursive {
  def apply[F[_]: Sync](parent: Option[MailFolder])(implicit
      c: Conv[Folder, MailFolder]
  ): MailOp[F, JavaMailConnection, Vector[MailFolder]] =
    MailOp(conn =>
      Sync[F].blocking {
        listRecursive(
          parent
            .map(pf => conn.store.getFolder(pf.id))
            .getOrElse(conn.store.getDefaultFolder)
        )
          .map(c.convert)
          .toVector
      }
    )

  private def listRecursive(
      folder: Folder
  ): List[Folder] = {
    val list = folder.list().toList
    if (list.isEmpty) {
      list
    } else {
      list ++ list.flatMap(f => listRecursive(f))
    }
  }
}
