package emil

final case class Recipients(
    to: List[MailAddress],
    cc: List[MailAddress],
    bcc: List[MailAddress]
) {

  def addTo(ma: MailAddress): Recipients =
    Recipients(ma :: to, cc, bcc)

  def addTos(ma: Seq[MailAddress]): Recipients =
    Recipients(ma.toList ::: to, cc, bcc)

  def addCc(ma: MailAddress): Recipients =
    Recipients(to, ma :: cc, bcc)

  def addCcs(ma: Seq[MailAddress]): Recipients =
    Recipients(to, ma.toList ::: cc, bcc)

  def addBcc(ma: MailAddress): Recipients =
    Recipients(to, cc, ma :: bcc)

  def addBccs(ma: Seq[MailAddress]): Recipients =
    Recipients(to, cc, ma.toList ::: bcc)

  def isEmpty: Boolean = to.isEmpty && cc.isEmpty && bcc.isEmpty

  def nonEmpty: Boolean = !isEmpty
}

object Recipients {

  val empty: Recipients = Recipients(Nil, Nil, Nil)

  def to(ma: MailAddress): Recipients =
    Recipients(List(ma), Nil, Nil)
}
