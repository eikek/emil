package emil.jsoup

import org.jsoup.safety._

/** A even more relaxed whitelist. It tries to retain all formatting
  * information, since e-mails usually contain a lot of `style' tags,
  * which is in contrast to websites. It throws away all non http/s
  * protocol images/links.
  *
  * The jsoup `Whitelist' contains more strict variants.
  */
object EmailWhitelist {

  val default =
    Safelist.relaxed
      .addTags("font", "center")
      .addAttributes(
        ":all",
        "style",
        "align",
        "valign",
        "width",
        "height",
        "border",
        "cellspacing",
        "cellpadding",
        "bgcolor",
        "color",
        "nowrap"
      )
      .addAttributes("font", "face", "size")

}
