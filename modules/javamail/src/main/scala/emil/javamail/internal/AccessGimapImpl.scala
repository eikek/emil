package emil.javamail.internal

import cats.effect.{Blocker, ContextShift, Sync}
import com.sun.mail.gimap.{GmailFolder, GmailStore}
import emil.javamail.internal.BlockingSyntax._
import emil.javamail.internal.ops.{MoveMail, SearchMails}
import emil.{AccessImap, MailHeader, MailOp}
import jakarta.mail.Transport

class AccessGimapImpl[F[_]: Sync: ContextShift](blocker: Blocker)
    extends AccessImapImpl[F](blocker)
    with AccessImap[F, JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder]] {

  def getGmailLabels(mh: MailHeader): MailOp[
    F,
    JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder],
    Set[GmailLabel]
  ] =
    SearchMails.getGmailLabels(mh).blockOn(blocker)

  def setGmailLabels(
      mh: MailHeader,
      labels: Set[GmailLabel],
      set: Boolean
  ): MailOp[
    F,
    JavaMailConnectionGeneric[GmailStore, Transport, GmailFolder],
    Unit
  ] =
    MoveMail.setGmailLabels(mh, labels, set).blockOn(blocker)

}
