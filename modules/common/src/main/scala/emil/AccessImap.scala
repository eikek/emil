package emil

import scodec.bits.ByteVector

trait AccessImap[F[_], -C] extends Access[F, C] {

  def getFolderNextUid(folder: MailFolder): MailOp[F, C, MailUid]

  def getFolderUidValidity(folder: MailFolder): MailOp[F, C, MailFolderUidValidity]

  def loadMail(folder: MailFolder, uid: MailUid): MailOp[F, C, Option[Mail[F]]]

  def loadMail(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, C, List[Mail[F]]]

  def loadMail(
      folder: MailFolder,
      uids: Set[MailUid]
  ): MailOp[F, C, List[Mail[F]]]

  def loadMailRaw(
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, C, Map[MailHeader, ByteVector]]

  def loadMailRaw(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, C, Map[MailHeader, ByteVector]]

  def loadMailRaw(
      folder: MailFolder,
      uids: Set[MailUid]
  ): MailOp[F, C, Map[MailHeader, ByteVector]]
}
