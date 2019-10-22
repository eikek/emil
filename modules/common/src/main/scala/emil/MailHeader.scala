package emil

import java.time.Instant

final case class MailHeader( id: String
                     , messageId: Option[String]
                     , folder: Option[MailFolder]
                     , recipients: Recipients
                     , sender: Option[MailAddress]
                     , from: Option[MailAddress]
                     , replyTo: Option[MailAddress]
                     , receivedDate: Option[Instant]
                     , sentDate: Option[Instant]
                     , subject: String
                     , flags: Set[Flag]) {

  def mapRecipients(f: Recipients => Recipients): MailHeader =
    copy(recipients = f(recipients))

  def withSubject(text: String): MailHeader =
    copy(subject = text)

  def withMessageID(id: String): MailHeader =
    copy(messageId = Some(id))
}

object MailHeader {
  val headerNames =
    List( "Subject", "Message-Id", "From", "To", "Cc", "Bcc", "Reply-To", "Received-Date", "Date")

  val empty: MailHeader =
    MailHeader("", None, None, Recipients.empty, None, None, None, None, None, "", Set.empty)
}
