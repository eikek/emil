package emil.javamail.internal.ops

import cats.effect.Sync
import emil._
import emil.javamail.conv.Conv
import emil.javamail.internal.JavaMailConnection
import javax.mail.Folder

object FindFolder {

  def apply[F[_]: Sync](parent: Option[MailFolder], name: String)(
      implicit c: Conv[Folder, MailFolder]
  ): MailOp[F, JavaMailConnection, Option[MailFolder]] =
    MailOp(
      conn =>
        Sync[F].delay {
          val f = parent
            .map(pf => conn.store.getFolder(pf.id).getFolder(name))
            .getOrElse(conn.store.getFolder(name))
          Option(f).filter(_.exists()).map(c.convert)
        }
    )

}
