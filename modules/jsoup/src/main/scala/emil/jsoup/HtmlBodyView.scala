package emil.jsoup

import cats.Applicative
import cats.implicits._
import emil._
import scodec.bits.ByteVector
import org.jsoup.nodes._

/** Creates a valid html document given a `MailBody'.
  *
  * Often, html body parts are only html snippets, missing a valid
  * `html' declaration. If the body is text/plain, it is converted
  * into basic html. The `emil-markdown' module should be included to
  * supply a more sophisticated text->html conversion.
  */
object HtmlBodyView {

  def apply[F[_]: Applicative](
      body: MailBody[F],
      meta: Option[MailHeader],
      textToHtml: Option[String => String],
      modify: Option[Document => Document] = Some(
        BodyClean.whitelistClean(EmailWhitelist.default)
      )
  ): F[BodyContent] = {
    val strToHtml   = textToHtml.getOrElse(defaultTxtToHtml _)
    val htmlSnippet = body.htmlContent(textContentToHtml(strToHtml))
    htmlSnippet.map(snippet => toHtmlDocument(snippet, meta, modify.getOrElse(identity)))
  }

  private def toHtmlDocument(
      in: BodyContent,
      meta: Option[MailHeader],
      modify: Document => Document
  ): BodyContent = {
    val change: Document => Document =
      modify.andThen(meta.map(ammendMeta).getOrElse(identity))
    BodyClean.modifyContent(change)(in)
  }

  private def textContentToHtml(f: String => String): BodyContent => BodyContent = {
    case BodyContent.StringContent(str) =>
      BodyContent.StringContent(f(str))
    case c @ BodyContent.ByteContent(_, cs) =>
      val str = c.asString
      BodyContent(ByteVector.view(f(str).getBytes(c.charsetOrUtf8)), cs)
  }

  private def defaultTxtToHtml(str: String): String =
    str
      .split("\r?\n")
      .toList
      .map(l => if (l.isEmpty) "</p><p>" else l)
      .mkString("<p>", "\n", "</p>")

  private def ammendMeta(header: MailHeader)(doc: Document): Document = {
    val el = makeMeta(header)
    doc.body.prepend(el)
    doc
  }

  private def makeMeta(header: MailHeader): String = {
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

    s"""<div style="padding-bottom: 0.8em;">
     |<strong>From:</strong> <code>${mail(header.from)}</code><br>
     |<strong>To:</strong> <code>${mails(header.recipients.to)}</code><br>
     |<strong>Subject:</strong> <code>${header.subject}</code><br>
     |<strong>Date:</strong> <code>${header.date.map(_.toString).getOrElse("")}</code>
     |</div>
     |""".stripMargin
  }
}
