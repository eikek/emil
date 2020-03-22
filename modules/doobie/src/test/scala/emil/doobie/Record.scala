package emil.doobie

import emil._

case class Record(from: MailAddress, recipients: List[MailAddress], ssl: SSLType, mime: MimeType)
