package emil

final case class MailAddress private (name: Option[String], address: String) {

  def displayString: String =
    name match {
      case Some(n) =>
        s"$n <$address>"
      case None =>
        address
    }
}

object MailAddress {

  def unsafe(name: Option[String], address: String): MailAddress =
    new MailAddress(name, address)

}
