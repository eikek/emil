package emil.javamail.internal

import cats.effect.Sync
import com.sun.mail.gimap.{GmailFolder, GmailStore}
import emil.javamail.internal.ops.{MoveMail, SearchMails}
import emil.{AccessImap, MailHeader, MailOp}
import jakarta.mail.Transport

class AccessGimapImpl[F[_]: Sync]
    extends AccessImapImpl[F]
    with AccessImap[F, JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder]] {

  def getGmailLabels(mh: MailHeader): MailOp[
    F,
    JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder],
    Set[GmailLabel]
  ] =
    SearchMails.getGmailLabels(mh)

  def setGmailLabels(
      mh: MailHeader,
      labels: Set[GmailLabel],
      set: Boolean
  ): MailOp[
    F,
    JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder],
    Unit
  ] =
    MoveMail.setGmailLabels(mh, labels, set)

}
