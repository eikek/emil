package emil.doobie

import _root_.emil._

case class Record(
    from: MailAddress,
    recipients: List[MailAddress],
    ssl: SSLType,
    mime: MimeType
)
