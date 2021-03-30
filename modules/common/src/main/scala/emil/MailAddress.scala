package emil

import cats.Hash
import emil.MailAddress._

final case class MailAddress private (name: Option[String], address: String) {

  def displayString: String = twoPartDisplay(name, address)

  override def hashCode(): Int = address.hashCode()

  override def equals(that: Any): Boolean = that match {
    case that: AnyRef if this.eq(that) => true
    case that: MailAddress             => this.address == that.address
    case _                             => false
  }

}

object MailAddress {

  def unsafe(name: Option[String], address: String): MailAddress =
    new MailAddress(name, address)

  implicit lazy val hash: Hash[MailAddress] = Hash.fromUniversalHashCode[MailAddress]

  private[emil] def twoPartDisplay(name: Option[String], address: String): String =
    name match {
      case Some(n) =>
        s"$n <$address>"
      case None =>
        address
    }
}
