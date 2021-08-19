package emil.jsoup

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import emil.jsoup.HtmlBodyViewConfig.Defaults
import org.jsoup.nodes._

/** Settings for creating a html document from a mail body.
  *
  * @param textToHtml
  *   in case there is only a plain text body, it must be converted to html. The default
  *   is a very simple approach, it is recommended to use the emil-markdown module for a
  *   more sophisticated variant.
  * @param modify:
  *   Allows to modify the parsed html tree. By default a cleaner is used with the default
  *   `EmailWhiteList`
  * @param dateFormat
  *   If a mail header is supplied, html is amended with the datetime where this formatter
  *   is used
  * @param zone
  *   the timezone to use when formatting the datetime
  */
case class HtmlBodyViewConfig(
    textToHtml: String => String = Defaults.defaultTxtToHtml,
    modify: Document => Document = Defaults.defaultModify,
    dateFormat: DateTimeFormatter = Defaults.defaultDateFormatter,
    zone: ZoneId = ZoneId.of("UTC")
)

object HtmlBodyViewConfig {

  val default = HtmlBodyViewConfig()

  object Defaults {
    val defaultTxtToHtml: String => String =
      _.split("\r?\n").toList
        .map(Entities.escape)
        .map(l => if (l.isEmpty) "</p><p>" else l)
        .mkString("<p>", "\n", "</p>")

    val defaultDateFormatter: DateTimeFormatter =
      DateTimeFormatter.RFC_1123_DATE_TIME

    val defaultModify: Document => Document =
      BodyClean.whitelistClean(EmailWhitelist.default)
  }
}
