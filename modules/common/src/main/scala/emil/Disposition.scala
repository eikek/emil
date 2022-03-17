package emil

sealed trait Disposition {def name: String}

/**
  * Enumeration of disposition types for a Content-Disposition part header.
  */
object Disposition {
  case object Inline extends Disposition {val name = "inline"}
  case object Attachment extends Disposition {val name = "attachment"}

  val values = Seq(Inline, Attachment)

  /**
    * Fail save disposition parser.
    *
    * @param name name of the disposition type to parse
    * @return found disposition type or None
    */
  def withName(name: String): Option[Disposition] = values.find(v => v.name.equalsIgnoreCase(name))

}
