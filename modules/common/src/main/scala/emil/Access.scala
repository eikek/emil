package emil

import cats.{Applicative, FlatMap}
import scodec.bits.ByteVector

trait Access[F[_], C] {

  def getInbox: MailOp[F, C, MailFolder]

  def createFolder(parent: Option[MailFolder], name: String): MailOp[F, C, MailFolder]

  def findFolder(
      parent: Option[MailFolder],
      name: String
  ): MailOp[F, C, Option[MailFolder]]

  def getOrCreateFolder(
      parent: Option[MailFolder],
      name: String
  )(implicit ev0: FlatMap[F], ev1: Applicative[F]): MailOp[F, C, MailFolder] =
    findFolder(parent, name).flatMap {
      case Some(mf) => MailOp.pure(mf)
      case None     => createFolder(parent, name)
    }

  def getMessageCount(folder: MailFolder): MailOp[F, C, Int]

  def search(folder: MailFolder, max: Int)(
      query: SearchQuery
  ): MailOp[F, C, SearchResult[MailHeader]]

  def searchAndLoad(folder: MailFolder, max: Int)(
      query: SearchQuery
  ): MailOp[F, C, SearchResult[Mail[F]]]

  def loadMail(mh: MailHeader): MailOp[F, C, Option[Mail[F]]]

  def loadMailRaw(mh: MailHeader): MailOp[F, C, Option[ByteVector]]

  def moveMail(mh: MailHeader, target: MailFolder): MailOp[F, C, Unit]

  def copyMail(mh: MailHeader, target: MailFolder): MailOp[F, C, Unit]

  def putMail(mh: Mail[F], target: MailFolder): MailOp[F, C, Unit]

  def deleteMails(mhs: Seq[MailHeader]): MailOp[F, C, DeleteResult]

  def deleteMail(mh: MailHeader*): MailOp[F, C, DeleteResult] =
    deleteMails(mh)

  def searchDelete(folder: MailFolder, max: Int)(
      query: SearchQuery
  )(implicit ev: FlatMap[F]): MailOp[F, C, DeleteResult] =
    search(folder, max)(query).flatMap(result => deleteMails(result.mails))
}
