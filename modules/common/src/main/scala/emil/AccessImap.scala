package emil

import scodec.bits.ByteVector

trait AccessImap[F[_], -C] extends Access[F, C] {

  def getFolderNextUid(folder: MailFolder): MailOp[F, C, MailUid]

  def getFolderUidValidity(folder: MailFolder): MailOp[F, C, MailUidValidity]

  def loadMail(folder: MailFolder, uid: MailUid): MailOp[F, C, Option[Mail[F]]]

  def loadMail(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, C, List[Mail[F]]]

  def loadMail(folder: MailFolder, uids: List[MailUid]): MailOp[F, C, List[Mail[F]]]

  def loadMailRaw(folder: MailFolder, uid: MailUid): MailOp[F, C, Option[ByteVector]]

  def loadMailRaw(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, C, List[ByteVector]]

  def loadMailRaw(folder: MailFolder, uids: List[MailUid]): MailOp[F, C, List[ByteVector]]
}
