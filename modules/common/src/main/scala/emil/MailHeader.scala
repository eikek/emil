package emil

import java.time.Instant

final case class MailHeader(
    id: String,
    messageId: Option[String],
    folder: Option[MailFolder],
    recipients: Recipients,
    sender: Option[MailAddress],
    from: Option[MailAddress],
    replyTo: Option[MailAddress],
    originationDate: Option[Instant],
    subject: String,
    received: List[Received],
    flags: Set[Flag]
) {

  def mapRecipients(f: Recipients => Recipients): MailHeader =
    copy(recipients = f(recipients))

  def withSubject(text: String): MailHeader =
    copy(subject = text)

  def withMessageID(id: String): MailHeader =
    copy(messageId = Some(id))

  def withOriginationDate(date: Instant): MailHeader =
    copy(originationDate = Some(date))

  /** Return the date closest to the received date. It is the latest
    * date from a `Received' headers and the originationDate (which
    * usually corresponds to the sent date).
    */
  def date: Option[Instant] =
    (received.map(_.date) ++ originationDate).sortBy(-_.toEpochMilli).headOption
}

object MailHeader {
  val headerNames =
    List("Subject", "Message-Id", "From", "To", "Cc", "Bcc", "Reply-To", "Date")

  val empty: MailHeader =
    MailHeader(
      "",
      None,
      None,
      Recipients.empty,
      None,
      None,
      None,
      None,
      "",
      Nil,
      Set.empty
    )
}
