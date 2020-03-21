package emil.markdown

case class MarkdownConfig(internalCss: String)

object MarkdownConfig {

  val defaultConfig = MarkdownConfig("""
        body { padding: 2em 5em; }
  """)

}
