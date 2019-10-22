package emil

sealed trait SSLType {
}
object SSLType {

  case object SSL extends SSLType

  case object StartTLS extends SSLType

  case object NoEncryption extends SSLType

}