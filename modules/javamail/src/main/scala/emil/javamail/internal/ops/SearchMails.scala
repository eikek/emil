package emil.javamail.internal.ops

import cats.effect.Sync
import com.sun.mail.gimap.{GmailFolder, GmailMessage, GmailStore}
import emil._
import emil.javamail.conv.Conv
import emil.javamail.internal.{
  GmailLabel,
  JavaMailConnection,
  JavaMailConnectionGeneric,
  Logger
}
import jakarta.mail.internet.MimeMessage
import jakarta.mail.search.SearchTerm
import jakarta.mail.{Flags, Folder, Transport}

object SearchMails {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](folder: MailFolder, q: SearchQuery, max: Int)(implicit
      cq: Conv[SearchQuery, SearchTerm],
      ch: Conv[MimeMessage, MailHeader]
  ): MailOp[F, JavaMailConnection, SearchResult[MailHeader]] =
    search[F, MailHeader](folder, q, max)

  def load[F[_]: Sync](folder: MailFolder, q: SearchQuery, max: Int)(implicit
      cq: Conv[SearchQuery, SearchTerm],
      ch: Conv[MimeMessage, Mail[F]]
  ): MailOp[F, JavaMailConnection, SearchResult[Mail[F]]] =
    search[F, Mail[F]](folder, q, max)

  def delete[F[_]: Sync](folder: MailFolder, q: SearchQuery, max: Int)(implicit
      cq: Conv[SearchQuery, SearchTerm]
  ): MailOp[F, JavaMailConnection, DeleteResult] =
    MailOp(conn =>
      conn
        .folder[F](folder.id, mode = Folder.READ_WRITE)
        .use(f =>
          Sync[F].delay {
            logger.debug(s"Searching for deletion: $q")
            val messages =
              if (q == SearchQuery.All) f.getMessages
              else f.search(cq.convert(q))
            f.setFlags(messages.take(max), new Flags(Flags.Flag.DELETED), true)
            val count = math.min(max, messages.length)
            logger.debug(s"Deleted $count message")
            DeleteResult(count)
          }
        )
    )

  def search[F[_]: Sync, B](folder: MailFolder, q: SearchQuery, max: Int)(implicit
      cq: Conv[SearchQuery, SearchTerm],
      ch: Conv[MimeMessage, B]
  ): MailOp[F, JavaMailConnection, SearchResult[B]] =
    MailOp(conn =>
      conn
        .folder[F](folder.id)
        .use(f =>
          Sync[F].delay {
            logger.debug(s"Searching: $q (max: $max)")
            val messages =
              if (q == SearchQuery.All) f.getMessages
              else f.search(cq.convert(q))
            SearchResult(
              messages
                .take(max)
                .toVector
                .map(_.asInstanceOf[MimeMessage])
                .map(ch.convert),
              messages.length
            )
          }
        )
    )

  def getGmailLabels[F[_]: Sync](mh: MailHeader): MailOp[
    F,
    JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder],
    Set[GmailLabel]
  ] = FindMail(mh).map(
    _.map(_.asInstanceOf[GmailMessage]).toList
      .flatMap(m => m.getLabels.map(GmailLabel))
      .toSet
  )

}
