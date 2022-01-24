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
        listRecursiveExcludeFolder(
          parent
            .map(pf => conn.store.getFolder(pf.id))
            .getOrElse(conn.store.getDefaultFolder)
        ).map(c.convert)
      }
    )

  def listRecursiveExcludeFolder(folder: Folder): Vector[Folder] = listRecursive(
    folder.list().toVector
  )

  @annotation.tailrec
  def listRecursive(
      folder: Vector[Folder],
      result: Vector[Folder] = Vector.empty
  ): Vector[Folder] =
    if (folder.isEmpty) result
    else listRecursive(folder.flatMap(_.list().toVector), folder ++ result)
}
