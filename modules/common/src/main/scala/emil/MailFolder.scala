package emil

import cats.Hash

final case class MailFolder(id: String, name: String)

object MailFolder {
  implicit lazy val hash: Hash[MailFolder] = Hash.fromUniversalHashCode[MailFolder]
}
