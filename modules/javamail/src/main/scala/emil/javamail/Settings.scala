package emil.javamail

/** Additional connection settings.
  */
final case class Settings(
  debug: Boolean,
  props: String => Map[String, String]
)

object Settings {

  val defaultSettings = Settings(false, _ => Map.empty)

}
