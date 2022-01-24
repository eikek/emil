package emil

import cats.{Hash, Show}

final case class MailFolderUidValidity(n: Long)

object MailFolderUidValidity {
  implicit lazy val hash: Hash[MailFolderUidValidity] =
    Hash.fromUniversalHashCode[MailFolderUidValidity]
  implicit lazy val show: Show[MailFolderUidValidity] =
    Show.fromToString[MailFolderUidValidity]
}
