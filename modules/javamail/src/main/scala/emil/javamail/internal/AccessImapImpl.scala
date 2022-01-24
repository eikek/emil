package emil.javamail.internal

import cats.effect.Sync
import emil._
import emil.javamail.conv.codec._
import emil.javamail.internal.ops._
import scodec.bits.ByteVector

class AccessImapImpl[F[_]: Sync]
    extends AccessImpl
    with AccessImap[F, JavaMailImapConnection] {

  def getFolderNextUid(folder: MailFolder): MailOp[F, JavaMailImapConnection, MailUid] =
    MailOp[F, JavaMailImapConnection, MailUid] {
      _.folder[F](folder.id).use { f =>
        Sync[F].blocking(MailUid(f.getUIDNext))
      }
    }

  def getFolderUidValidity(
      folder: MailFolder
  ): MailOp[F, JavaMailImapConnection, MailUidValidity] =
    MailOp[F, JavaMailImapConnection, MailUidValidity] {
      _.folder[F](folder.id).use { f =>
        Sync[F].blocking(MailUidValidity(f.getUIDValidity))
      }
    }

  def loadMail(
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, JavaMailImapConnection, Option[Mail[F]]] =
    LoadMail.byUid[F](folder, uid)

  def loadMail(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, JavaMailImapConnection, List[Mail[F]]] =
    LoadMail.byUid[F](folder, start, end)

  def loadMail(
      folder: MailFolder,
      uids: List[MailUid]
  ): MailOp[F, JavaMailImapConnection, List[Mail[F]]] =
    LoadMail.byUid[F](folder, uids)

  def loadMailRaw(
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, JavaMailImapConnection, Option[ByteVector]] =
    LoadMailRaw.byUid[F](folder, uid)

  def loadMailRaw(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, JavaMailImapConnection, List[ByteVector]] =
    LoadMailRaw.byUid[F](folder, start, end)

  def loadMailRaw(
      folder: MailFolder,
      uids: List[MailUid]
  ): MailOp[F, JavaMailImapConnection, List[ByteVector]] =
    LoadMailRaw.byUid[F](folder, uids)

}
