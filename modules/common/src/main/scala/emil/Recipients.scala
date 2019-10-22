package emil

final case class Recipients(to: List[MailAddress], cc: List[MailAddress], bcc: List[MailAddress]) {

  def addTo(ma: MailAddress): Recipients =
    Recipients(ma :: to, cc, bcc)

  def addCc(ma: MailAddress): Recipients =
    Recipients(to, ma :: cc, bcc)

  def addBcc(ma: MailAddress): Recipients =
    Recipients(to, cc, ma :: bcc)
}

object Recipients {

  val empty: Recipients = Recipients(Nil, Nil, Nil)

  def to(ma: MailAddress): Recipients =
    Recipients(List(ma), Nil, Nil)
}
