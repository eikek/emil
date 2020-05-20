package emil.javamail

/** Additional connection settings.
  */
final case class Settings(
    debug: Boolean,
    enableXOAuth2: Boolean,
    props: String => Map[String, String]
)

object Settings {

  val defaultSettings = Settings(false, true, _ => Map.empty)

}
