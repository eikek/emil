package emil

import cats.Hash

sealed trait SSLType { self: Product =>

  final def name: String =
    productPrefix.toLowerCase

}

object SSLType {

  case object SSL extends SSLType
  case object StartTLS extends SSLType
  case object NoEncryption extends SSLType

  def fromString(str: String): Either[String, SSLType] =
    str.toLowerCase match {
      case "ssl"          => Right(SSL)
      case "starttls"     => Right(StartTLS)
      case "noencryption" => Right(NoEncryption)
      case "none"         => Right(NoEncryption)
      case _              => Left(s"Invalid ssl type: $str")
    }

  def unsafe(str: String): SSLType =
    fromString(str).fold(sys.error, identity)

  implicit lazy val hash: Hash[SSLType] = Hash.fromUniversalHashCode[SSLType]

}
