package emil.javamail.internal

import cats.FlatMap
import cats.effect.Sync
import cats.implicits._
import emil._
import emil.javamail.conv.codec._
import emil.javamail.internal.ops._

final class AccessImpl[F[_]: Sync] extends Access[F, JavaMailConnection] {
  def getInbox: MailOp[F, JavaMailConnection, MailFolder] =
    FindFolder(None, "INBOX")
      .andThen(optMf =>
        optMf match {
          case Some(mf) => mf.pure[F]
          case None => Sync[F].raiseError(new Exception("Folder INBOX doesn't exist."))
        }
      )

  def createFolder(
      parent: Option[MailFolder],
      name: String
  ): MailOp[F, JavaMailConnection, MailFolder] =
    CreateFolder(parent, name)

  def findFolder(
      parent: Option[MailFolder],
      name: String
  ): MailOp[F, JavaMailConnection, Option[MailFolder]] =
    FindFolder[F](parent, name)

  def getMessageCount(folder: MailFolder): MailOp[F, JavaMailConnection, Int] =
    MailOp(conn =>
      conn.folder[F](folder.id).use(f => Sync[F].blocking(f.getMessageCount))
    )

  def search(folder: MailFolder, max: Int)(
      query: SearchQuery
  ): MailOp[F, JavaMailConnection, SearchResult[MailHeader]] =
    SearchMails(folder, query, max)

  def searchAndLoad(folder: MailFolder, max: Int)(
      query: SearchQuery
  ): emil.MailOp[F, JavaMailConnection, SearchResult[Mail[F]]] =
    SearchMails.load(folder, query, max)

  override def searchDelete(folder: MailFolder, max: Int)(
      query: SearchQuery
  )(implicit ev: FlatMap[F]): MailOp[F, JavaMailConnection, DeleteResult] =
    SearchMails.delete(folder, query, max)

  def loadMail(mh: MailHeader): MailOp[F, JavaMailConnection, Option[Mail[F]]] =
    LoadMail(mh)

  def moveMail(mh: MailHeader, target: MailFolder): MailOp[F, JavaMailConnection, Unit] =
    MoveMail(mh, target)

  def copyMail(mh: MailHeader, target: MailFolder): MailOp[F, JavaMailConnection, Unit] =
    CopyMail(mh, target)

  def putMail(mail: Mail[F], target: MailFolder): MailOp[F, JavaMailConnection, Unit] =
    PutMail(mail, target)

  def deleteMails(mhs: Seq[MailHeader]): MailOp[F, JavaMailConnection, DeleteResult] =
    mhs.toVector.traverse(DeleteMail[F]).map(v => DeleteResult(v.sum))

  def listFolders(
      parent: Option[MailFolder]
  ): MailOp[F, JavaMailConnection, Vector[MailFolder]] =
    ListFolders[F](parent)
}
