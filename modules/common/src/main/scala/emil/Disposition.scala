package emil

import cats.Hash

sealed trait Disposition { self: Product =>
  final def name: String = productPrefix.toLowerCase
}

/** Enumeration of disposition types for a Content-Disposition part header. */
object Disposition {
  case object Inline extends Disposition
  case object Attachment extends Disposition

  def fromString(str: String): Either[String, Disposition] =
    (if (str == null) null else str.toLowerCase) match {
      case "inline"     => Right(Inline)
      case "attachment" => Right(Attachment)
      case _            => Left(s"Invalid disposition type: '$str'")
    }

  def unsafe(str: String): Disposition =
    fromString(str).fold(sys.error, identity)

  implicit lazy val hash: Hash[Disposition] = Hash.fromUniversalHashCode[Disposition]
}
