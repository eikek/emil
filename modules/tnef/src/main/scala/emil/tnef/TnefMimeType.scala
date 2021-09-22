package emil.tnef

import emil._

object TnefMimeType {

  val applicationTnef = MimeType.application("ms-tnef")
  val applicationVndTnef = MimeType.application("vnd.ms-tnef")

  val all = Set(applicationTnef, applicationVndTnef)

  def matches(mt: MimeType): Boolean =
    mt.primary == "application" && mt.sub.contains("tnef")
}
