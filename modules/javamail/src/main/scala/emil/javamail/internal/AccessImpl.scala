package emil.javamail.internal

import cats.FlatMap
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import emil._
import emil.javamail.conv.codec._
import emil.javamail.internal.BlockingSyntax._
import emil.javamail.internal.ops._
import jakarta.mail.UIDFolder
import scodec.bits.ByteVector

final class AccessImpl[F[_]: Sync: ContextShift](blocker: Blocker)
    extends Access[F, JavaMailConnection] {
  def getInbox: MailOp[F, JavaMailConnection, MailFolder] =
    FindFolder(None, "INBOX")
      .blockOn(blocker)
      .andThen(optMf =>
        optMf match {
          case Some(mf) => mf.pure[F]
          case None     => Sync[F].raiseError(new Exception("Folder INBOX doesn't exist."))
        }
      )

  def createFolder(
      parent: Option[MailFolder],
      name: String
  ): MailOp[F, JavaMailConnection, MailFolder] =
    CreateFolder(parent, name).blockOn(blocker)

  def findFolder(
      parent: Option[MailFolder],
      name: String
  ): MailOp[F, JavaMailConnection, Option[MailFolder]] =
    FindFolder[F](parent, name).blockOn(blocker)

  def getMessageCount(folder: MailFolder): MailOp[F, JavaMailConnection, Int] =
    MailOp(conn => conn.folder[F](folder.id).use(f => Sync[F].delay(f.getMessageCount)))

  def getFolderNextUid(folder: MailFolder): MailOp[F, JavaMailConnection, MailUid] =
    MailOp[F, JavaMailConnection, MailUid] {
      _.folder[F](folder.id).use { f =>
        Sync[F].delay {
          MailUid(f.asInstanceOf[UIDFolder].getUIDNext)
        }
      }
    }.blockOn(blocker)

  def getFolderUidValidity(
      folder: MailFolder
  ): MailOp[F, JavaMailConnection, MailUidValidity] =
    MailOp[F, JavaMailConnection, MailUidValidity] {
      _.folder[F](folder.id).use { f =>
        Sync[F].delay {
          MailUidValidity(f.asInstanceOf[UIDFolder].getUIDValidity)
        }
      }
    }.blockOn(blocker)

  def search(folder: MailFolder, max: Int)(
      query: SearchQuery
  ): MailOp[F, JavaMailConnection, SearchResult[MailHeader]] =
    SearchMails(folder, query, max).blockOn(blocker)

  def searchAndLoad(folder: MailFolder, max: Int)(
      query: SearchQuery
  ): emil.MailOp[F, JavaMailConnection, SearchResult[Mail[F]]] =
    SearchMails.load(folder, query, max)

  override def searchDelete(folder: MailFolder, max: Int)(
      query: SearchQuery
  )(implicit ev: FlatMap[F]): MailOp[F, JavaMailConnection, DeleteResult] =
    SearchMails.delete(folder, query, max).blockOn(blocker)

  def loadMail(mh: MailHeader): MailOp[F, JavaMailConnection, Option[Mail[F]]] =
    LoadMail(mh).blockOn(blocker)

  override def loadMail(
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, JavaMailConnection, Option[Mail[F]]] =
    LoadMail.byUid[F](folder, uid).blockOn(blocker)

  override def loadMail(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, JavaMailConnection, List[Mail[F]]] =
    LoadMail.byUid[F](folder, start, end).blockOn(blocker)

  override def loadMail(
      folder: MailFolder,
      uids: List[MailUid]
  ): MailOp[F, JavaMailConnection, List[Mail[F]]] =
    LoadMail.byUid[F](folder, uids).blockOn(blocker)

  def loadMailRaw(mh: MailHeader): MailOp[F, JavaMailConnection, Option[ByteVector]] =
    LoadMailRaw(mh).blockOn(blocker)

  override def loadMailRaw(
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, JavaMailConnection, Option[ByteVector]] =
    LoadMailRaw.byUid[F](folder, uid).blockOn(blocker)

  override def loadMailRaw(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, JavaMailConnection, List[ByteVector]] =
    LoadMailRaw.byUid[F](folder, start, end).blockOn(blocker)

  override def loadMailRaw(
      folder: MailFolder,
      uids: List[MailUid]
  ): MailOp[F, JavaMailConnection, List[ByteVector]] =
    LoadMailRaw.byUid[F](folder, uids).blockOn(blocker)

  def moveMail(mh: MailHeader, target: MailFolder): MailOp[F, JavaMailConnection, Unit] =
    MoveMail(mh, target).blockOn(blocker)

  def copyMail(mh: MailHeader, target: MailFolder): MailOp[F, JavaMailConnection, Unit] =
    CopyMail(mh, target).blockOn(blocker)

  def putMail(mail: Mail[F], target: MailFolder): MailOp[F, JavaMailConnection, Unit] =
    PutMail(mail, target)

  def deleteMails(mhs: Seq[MailHeader]): MailOp[F, JavaMailConnection, DeleteResult] =
    mhs.toVector.traverse(DeleteMail[F]).blockOn(blocker).map(v => DeleteResult(v.sum))
}
