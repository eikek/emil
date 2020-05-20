package emil

import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import scodec.bits.ByteVector

/** Content part of the mail body.
  *
  * The mail body may be alternative content of text/plain, text/html
  * or both. A body content may be specified by a String or a
  * byte-array + charset. For encoding body contents from a string,
  * the UTF-8 encoding is used.
  *
  * The byte-array variant is then needed, if a mail is decoded from a
  * mime-representation. A content part may use different character
  * encodings and a html part may specifiy this encoding in its
  * content, too. Thus it sometimes is necessary to get to the same
  * bytes as in the original mail.
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
        bytes.decodeString(cs)
      case None =>
        bytes.decodeUtf8
    }

  def isEmpty: Boolean

  def nonEmpty: Boolean = !isEmpty
}

object BodyContent {
  val UTF8 = StandardCharsets.UTF_8

  val empty = apply("")

  def apply(str: String): BodyContent =
    StringContent(str)

  def apply(bytes: ByteVector, charset: Option[Charset]): BodyContent =
    ByteContent(bytes, charset)

  final case class StringContent(asString: String) extends BodyContent {
    lazy val bytes               = ByteVector.view(asString.getBytes(UTF8))
    val charset: Option[Charset] = Some(UTF8)

    def isEmpty: Boolean = asString.isEmpty
  }

  final case class ByteContent(bytes: ByteVector, charset: Option[Charset])
      extends BodyContent {
    def asString         = contentDecode.fold(throw _, identity)
    def isEmpty: Boolean = bytes.isEmpty
  }
}
