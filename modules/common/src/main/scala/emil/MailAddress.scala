package emil

final case class MailAddress private (name: Option[String], address: String) {}

object MailAddress {

  def unsafe(name: Option[String], address: String): MailAddress =
    new MailAddress(name, address)

}
