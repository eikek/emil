package emil.jsoup

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import cats.Applicative
import cats.implicits._
import emil._
import org.jsoup.nodes._
import scodec.bits.ByteVector

/** Creates a valid html document given a `MailBody'.
  *
  * Often, html body parts are only html snippets, missing a valid `html' declaration. If
  * the body is text/plain, it is converted into basic html. The `emil-markdown' module
  * should be included to supply a more sophisticated text->html conversion.
  */
object HtmlBodyView {

  def apply[F[_]: Applicative](
      body: MailBody[F],
      meta: Option[MailHeader],
      config: HtmlBodyViewConfig = HtmlBodyViewConfig()
  ): F[BodyContent] = {
    val htmlSnippet = body.htmlContent(textContentToHtml(config.textToHtml))
    htmlSnippet.map(snippet => toHtmlDocument(snippet, meta, config))
  }

  private def toHtmlDocument(
      in: BodyContent,
      meta: Option[MailHeader],
      cfg: HtmlBodyViewConfig
  ): BodyContent = {
    val change: Document => Document =
      cfg.modify.andThen(meta.map(amendMeta(cfg)).getOrElse(identity))
    BodyClean.modifyContent(change)(in)
  }

  private def textContentToHtml(f: String => String): BodyContent => BodyContent = {
    case BodyContent.StringContent(str) =>
      BodyContent.StringContent(f(str))
    case c @ BodyContent.ByteContent(_, cs) =>
      val str = c.asString
      BodyContent(ByteVector.view(f(str).getBytes(c.charsetOrUtf8)), cs)
  }

  private def amendMeta(
      cfg: HtmlBodyViewConfig
  )(header: MailHeader)(doc: Document): Document = {
    val el = makeMeta(header, cfg.dateFormat, cfg.zone)
    doc.body.prepend(el)
    doc
  }

  private def makeMeta(
      header: MailHeader,
      fmt: DateTimeFormatter,
      zone: ZoneId
  ): String = {
    def mail(ma: Option[MailAddress]): String =
      ma.map(_.displayString).map(Entities.escape).getOrElse("-")

    def mails(mas: List[MailAddress]): String =
      mas match {
        case Nil => "-"
        case _ =>
          mas
            .map(_.displayString)
            .map(Entities.escape)
            .mkString(", ")
      }

    val dateStr = header.date.map(_.atZone(zone).format(fmt)).getOrElse("-")

    s"""<div style="padding-bottom: 0.8em;">
     |<strong>From:</strong> <code>${mail(header.from)}</code><br>
     |<strong>To:</strong> <code>${mails(header.recipients.to)}</code><br>
     |<strong>Subject:</strong> <code>${header.subject}</code><br>
     |<strong>Date:</strong> <code>$dateStr</code>
     |</div>
     |""".stripMargin
  }
}
