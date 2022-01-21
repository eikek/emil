package emil

import cats.{Hash, Show}

final case class MailUidValidity(n: Long)

object MailUidValidity {
  implicit lazy val hash: Hash[MailUidValidity] =
    Hash.fromUniversalHashCode[MailUidValidity]
  implicit lazy val show: Show[MailUidValidity] = Show.fromToString[MailUidValidity]
}
