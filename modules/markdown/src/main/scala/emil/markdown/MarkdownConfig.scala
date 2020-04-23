package emil.markdown

case class MarkdownConfig(internalCss: String)

object MarkdownConfig {

  val defaultConfig = MarkdownConfig("""
        body { font-size: 10pt; font-family: sans-serif; }
  """)

}
