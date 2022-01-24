package emil.javamail.internal.ops

import cats.effect.Sync
import com.sun.mail.imap.IMAPFolder
import emil._
import emil.javamail.internal._
import jakarta.mail.internet.MimeMessage
import jakarta.mail.search.MessageIDTerm

object FindMail {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mh: MailHeader
  ): MailOp[F, JavaMailConnection, Option[MimeMessage]] =
    MailOp { conn =>
      Sync[F].blocking {
        val iid = InternalId.readInternalId(mh.id)
        logger.debug(s"About to find mail with internal id: $iid")
        iid.toOption.flatMap(id => findByInternalId(conn, mh, id))
      }
    }

  def byUid[F[_]: Sync](
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, JavaMailImapConnection, Option[MimeMessage]] = MailOp {
    _.folder[F](folder.id)
      .use { folder =>
        Sync[F].delay {
          logger.debug(s"About to find mail $uid")
          Option(folder.getMessageByUID(uid.n))
            .collect { case m: MimeMessage => m }
        }
      }
  }

  def byUid[F[_]: Sync](
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, JavaMailImapConnection, List[MimeMessage]] = MailOp {
    _.folder[F](folder.id)
      .use { folder =>
        Sync[F].delay {
          logger.debug(s"About to find mail from $start to $end")
          folder
            .getMessagesByUID(start.n, end.n)
            .collect { case m: MimeMessage => m }
            .toList
        }
      }
  }

  def byUid[F[_]: Sync](
      folder: MailFolder,
      uids: List[MailUid]
  ): MailOp[F, JavaMailImapConnection, List[MimeMessage]] = MailOp {
    _.folder[F](folder.id)
      .use { folder =>
        Sync[F].delay {
          logger.debug(s"About to find mail with $uids")
          folder
            .getMessagesByUID(uids.toArray.map(_.n))
            .collect { case m: MimeMessage => m }
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
