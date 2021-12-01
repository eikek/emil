package emil

import cats.Hash
import cats.data.NonEmptyList

/** Structure representing one mailbox (= folder).
  *
  * @param id
  *   Absolute path of the MailFolder
  * @param path
  *   Path segments of the MailFolder, the last segment resembles the folder's name
  */
final case class MailFolder(id: String, path: NonEmptyList[String]) {
  def name: String = path.last
}

object MailFolder {
  implicit lazy val hash: Hash[MailFolder] = Hash.fromUniversalHashCode[MailFolder]
}
