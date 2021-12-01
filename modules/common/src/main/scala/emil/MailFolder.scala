package emil

import cats.Hash

/** Structure representing one mailbox (= folder).
  *
  * @param id
  *   Absolute path of the MailFolder
  * @param name
  *   Display name (last path segment)
  * @param separator
  *   Hierarchy separator
  */
final case class MailFolder(id: String, name: String, separator: Char) {

  /** Splits the id (path) of a MailFolder in its segments using the separator.
    *
    * @note
    *   Since no escaping is supported in hierarchy paths, and only one type of separator
    *   is allowed in a hierarchy path, this is all the logic it needs to split the path
    *   into its segments.
    * @see
    *   https://datatracker.ietf.org/doc/html/rfc3501#section-5.1.1
    *
    * @return
    *   Separated path segments
    */
  def pathSegments: Array[String] = id.split(separator)
}

object MailFolder {
  implicit lazy val hash: Hash[MailFolder] = Hash.fromUniversalHashCode[MailFolder]
}
