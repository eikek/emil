package emil.javamail.internal.ops

import cats.effect.Sync
import com.sun.mail.imap.IMAPFolder
import emil._
import emil.javamail.internal.{InternalId, JavaMailConnection, Logger, Util}
import javax.mail.internet.MimeMessage
import javax.mail.search.MessageIDTerm

object FindMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](mh: MailHeader): MailOp[F, JavaMailConnection, Option[MimeMessage]] =
    MailOp.of { conn =>
      val iid = InternalId.readInternalId(mh.id)
      logger.debug(s"About to find mail with internal id: $iid")
      iid.toOption.flatMap(id => findByInternalId(conn, mh, id))
    }

  private def findByInternalId(
      conn: JavaMailConnection,
      mh: MailHeader,
      id: InternalId
  ): Option[MimeMessage] =
    id match {
      case InternalId.MessageId(id) =>
        findByMessageId(conn, mh, id)
      case InternalId.Uid(uid) =>
        findByUID(conn, mh, uid)
      case InternalId.NoId =>
        logger.warn(s"No id present. Cannot find mail")
        None
    }

  private def findByUID(
      conn: JavaMailConnection,
      mh: MailHeader,
      uid: Long
  ): Option[MimeMessage] = {
    val parent = mh.folder.map(_.id).getOrElse("INBOX");
    conn.store.getFolder(parent) match {
      case im: IMAPFolder if im.exists() =>
        Util.withReadFolder(im) { im =>
          logger.debug(s"Looking up message '$mh' by uid '$uid'")
          Option(im.getMessageByUID(uid).asInstanceOf[MimeMessage])
        }
      case _ =>
        logger.debug(s"No folder found for '$mh', cannot find message.")
        None
    }
  }

  private def findByMessageId(
      conn: JavaMailConnection,
      mh: MailHeader,
      mid: String
  ): Option[MimeMessage] = {
    val parent = mh.folder.map(_.id).getOrElse("INBOX");
    Option(conn.store.getFolder(parent))
      .filter(_.exists())
      .flatMap { f =>
        logger.debug(s"Looking for message '$mh' by MessageID '$mid'")
        val results = f.search(new MessageIDTerm(mid))
        results.headOption.map(_.asInstanceOf[MimeMessage])
      }
  }
}
