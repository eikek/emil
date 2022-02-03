package emil.javamail.internal

import cats.effect.{Blocker, ContextShift, Sync}
import com.sun.mail.gimap.{GmailFolder, GmailStore}
import emil.javamail.internal.BlockingSyntax._
import emil.javamail.internal.ops.{MoveMail, SearchMails}
import emil.{AccessImap, MailFolder, MailOp, MailUid}
import jakarta.mail.Transport

class AccessGimapImpl[F[_]: Sync: ContextShift](blocker: Blocker)
    extends AccessImapImpl[F](blocker)
    with AccessImap[F, JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder]] {

//  def loadMailRawGmail(
//      folder: MailFolder,
//      start: MailUid,
//      end: MailUid
//  ): MailOp[
//    F,
//    JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder],
//    Map[GmailMailCompositeId, ByteVector]
//  ] =
//    LoadMailRaw.byUidGmail[F](folder, start, end).blockOn(blocker)

  def getGmailLabels(folder: MailFolder, uid: MailUid): MailOp[
    F,
    JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder],
    Set[GmailLabel]
  ] =
    SearchMails.getGmailLabels(folder, uid).blockOn(blocker)

  def changeGmailLabels(
      folder: MailFolder,
      uid: MailUid,
      labels: Set[GmailLabel],
      set: Boolean
  ): MailOp[
    F,
    JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder],
    Unit
  ] =
    MoveMail.setGmailLabels(folder, uid, labels, set).blockOn(blocker)

}
