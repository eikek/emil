package emil

import java.nio.charset.{CharacterCodingException, Charset, StandardCharsets}

import cats.Hash
import scodec.bits.ByteVector

/** Content part of the mail body.
  *
  * The mail body may be alternative content of text/plain, text/html or both. A body
  * content may be specified by a String or a byte-array + charset. For encoding body
  * contents from a string, the UTF-8 encoding is used.
  *
  * The byte-array variant is then needed, if a mail is decoded from a
  * mime-representation. A content part may use different character encodings and a html
  * part may specify this encoding in its content, too. Thus it sometimes is necessary to
  * get to the same bytes as in the original mail.
  */
sealed trait BodyContent {

  def bytes: ByteVector

  def charset: Option[Charset]

  final def charsetOrUtf8 =
    charset.getOrElse(BodyContent.UTF8)

  def asString: String

  final def contentDecode: Either[CharacterCodingException, String] =
    charset match {
      case Some(cs) =>
        val e = bytes.decodeString(cs)
        //note: .orElse is 2.13 only
        e.fold(_ => PreferredCharsets.decode(bytes), _ => e)

      case None =>
        // try some, we don't know
        PreferredCharsets.decode(bytes)
    }

  def isEmpty: Boolean

  def nonEmpty: Boolean = !isEmpty

  final override def hashCode(): Int = bytes.hashCode

  final override def equals(that: Any): Boolean = that match {
    case that: AnyRef if this.eq(that) => true
    case that: BodyContent             => this.bytes == that.bytes
    case _                             => false
  }

}

object BodyContent {
  val UTF8 = StandardCharsets.UTF_8

  val empty = apply("")

  def apply(str: String): BodyContent =
    StringContent(str)

  def apply(bytes: ByteVector, charset: Option[Charset]): BodyContent =
    ByteContent(bytes, charset)

  implicit lazy val hash: Hash[BodyContent] = Hash.fromUniversalHashCode[BodyContent]

  final case class StringContent(asString: String) extends BodyContent {
    lazy val bytes = ByteVector.view(asString.getBytes(UTF8))
    val charset: Option[Charset] = Some(UTF8)

    def isEmpty: Boolean = asString.isEmpty
  }

  final case class ByteContent(bytes: ByteVector, charset: Option[Charset])
      extends BodyContent {
    def asString = contentDecode.fold(placeholder, identity)
    def isEmpty: Boolean = bytes.isEmpty

    private def placeholder(ex: Throwable): String =
      s"Error decoding ${bytes.size} bytes with charset '$charset': ${ex.getMessage}"
  }
}
