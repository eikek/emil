package emil.javamail.internal.ops

import cats.effect.Sync
import cats.syntax.all._
import com.sun.mail.imap._
import emil._
import emil.javamail.internal._
import jakarta.mail.internet.MimeMessage
import jakarta.mail.search.MessageIDTerm

object FindMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mh: MailHeader
  ): MailOp[F, JavaMailConnection, Option[MimeMessage]] =
    MailOp.of { conn =>
      val iid = InternalId.readInternalId(mh.id)
      logger.debug(s"About to find mail with internal id: $iid")
      iid.toOption.flatMap(id => findByInternalId(conn, mh, id))
    }

  def byUid[F[_]: Sync](
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, JavaMailImapConnection, Option[IMAPMessage]] = MailOp {
    _.folder[F](folder.id)
      .use { folder =>
        Sync[F].delay {
          logger.debug(s"About to find mail $uid")
          Option(folder.getMessageByUID(uid.n)).map(_.asInstanceOf[IMAPMessage])
        }
      }
  }

  def byUid[F[_]: Sync](
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, JavaMailImapConnection, List[IMAPMessage]] = MailOp {
    _.folder[F](folder.id)
      .use { folder =>
        Sync[F].delay {
          logger.debug(s"About to find mail from $start to $end")
          folder
            .getMessagesByUID(start.n, end.n)
            .map(_.asInstanceOf[IMAPMessage])
            .toList
        }
      }
  }

  def byUid[F[_]: Sync](
      folder: MailFolder,
      uids: Set[MailUid]
  ): MailOp[F, JavaMailImapConnection, List[IMAPMessage]] = MailOp {
    _.folder[F](folder.id)
      .use { folder =>
        Sync[F].delay {
          val uidsSorted = uids.toArray.sortBy(_.n)
          logger.debug(s"About to find mail with ${uidsSorted.toList}")
          folder
            .getMessagesByUID(uidsSorted.map(_.n))
            .map(_.asInstanceOf[IMAPMessage])
            .toList
        }
      }
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
