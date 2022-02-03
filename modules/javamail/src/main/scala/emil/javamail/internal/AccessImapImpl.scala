package emil.javamail.internal

import cats.effect.{Blocker, ContextShift, Sync}
import emil._
import emil.javamail.conv.codec._
import emil.javamail.internal.BlockingSyntax._
import emil.javamail.internal.ops._
import scodec.bits.ByteVector

class AccessImapImpl[F[_]: Sync: ContextShift](blocker: Blocker)
    extends AccessImpl(blocker)
    with AccessImap[F, JavaMailImapConnection] {

  def getFolderNextUid(folder: MailFolder): MailOp[F, JavaMailImapConnection, MailUid] =
    MailOp[F, JavaMailImapConnection, MailUid] {
      _.folder[F](folder.id).use { f =>
        Sync[F].delay(MailUid(f.getUIDNext))
      }
    }.blockOn(blocker)

  def getFolderUidValidity(
      folder: MailFolder
  ): MailOp[F, JavaMailImapConnection, MailUidValidity] =
    MailOp[F, JavaMailImapConnection, MailUidValidity] {
      _.folder[F](folder.id).use { f =>
        Sync[F].delay(MailUidValidity(f.getUIDValidity))
      }
    }.blockOn(blocker)

  def loadMail(
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, JavaMailImapConnection, Option[Mail[F]]] =
    LoadMail.byUid[F](folder, uid).blockOn(blocker)

  def loadMail(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, JavaMailImapConnection, Map[MailUid, Mail[F]]] =
    LoadMail.byUid[F](folder, start, end).blockOn(blocker)

  def loadMail(
      folder: MailFolder,
      uids: Set[MailUid]
  ): MailOp[F, JavaMailImapConnection, Map[MailUid, Mail[F]]] =
    LoadMail.byUid[F](folder, uids).blockOn(blocker)

  def loadMailRaw(
      folder: MailFolder,
      uid: MailUid
  ): MailOp[F, JavaMailImapConnection, Option[ByteVector]] =
    LoadMailRaw.byUid[F](folder, uid).blockOn(blocker)

  def loadMailRaw(
      folder: MailFolder,
      start: MailUid,
      end: MailUid
  ): MailOp[F, JavaMailImapConnection, Map[MailUid, ByteVector]] =
    LoadMailRaw.byUid[F](folder, start, end).blockOn(blocker)

  def loadMailRaw(
      folder: MailFolder,
      uids: Set[MailUid]
  ): MailOp[F, JavaMailImapConnection, Map[MailUid, ByteVector]] =
    LoadMailRaw.byUid[F](folder, uids).blockOn(blocker)

}
