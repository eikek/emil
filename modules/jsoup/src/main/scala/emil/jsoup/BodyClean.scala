package emil.jsoup

import cats.Applicative
import cats.implicits._
import emil._
import emil.builder.Trans
import org.jsoup._
import org.jsoup.safety._
import org.jsoup.nodes._
import scodec.bits.ByteVector
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

/** Modifies/cleans html bodies.
  */
case class BodyClean[F[_]: Applicative](change: Document => Document) extends Trans[F] {

  def apply(mail: Mail[F]): Mail[F] =
    mail.mapBody(BodyClean.modifyBody(change))

}

object BodyClean {

  /** Default `change' function for the BodyClean constructor.
    */
  def whitelistClean(whitelist: Whitelist): Document => Document =
    doc => {
      val cleaner = new Cleaner(whitelist)
      val body    = cleaner.clean(doc).body
      doc.body.replaceWith(body)
      doc
    }

  def apply[F[_]: Applicative](whitelist: Whitelist): Trans[F] =
    BodyClean[F](whitelistClean(whitelist))

  def default[F[_]: Applicative] = apply[F](EmailWhitelist.default)

  def cleanBody[F[_]: Applicative](whitelist: Whitelist)(body: MailBody[F]): MailBody[F] =
    modifyBody(whitelistClean(whitelist))(body)

  def modifyBody[F[_]: Applicative](
      change: Document => Document
  )(body: MailBody[F]): MailBody[F] =
    body match {
      case b @ MailBody.Empty() =>
        b

      case b @ MailBody.Text(_) =>
        b

      case MailBody.Html(html) =>
        MailBody.Html(html.map(modifyContent(change)))

      case MailBody.HtmlAndText(txt, html) =>
        MailBody.HtmlAndText(txt, html.map(modifyContent(change)))
    }

  def modifyContent(
      change: Document => Document
  )(html: BodyContent): BodyContent = {
    def changeDoc: Document => Document =
      fixCharset(html.charsetOrUtf8).andThen(change)

    html match {
      case BodyContent.StringContent(orig) =>
        val doc = Jsoup.parse(orig)
        BodyContent(changeDoc(doc).outerHtml)
      case BodyContent.ByteContent(bv, cs) =>
        val doc =
          Jsoup.parse(new ByteArrayInputStream(bv.toArray), cs.map(_.name).orNull, "")
        val newBytes = changeDoc(doc).outerHtml.getBytes(html.charsetOrUtf8)
        BodyContent.ByteContent(ByteVector.view(newBytes), cs)
    }
  }

  private[jsoup] def fixCharset(cs: Charset): Document => Document =
    doc => {
      doc.head.getElementsByAttributeValue("http-equiv", "content-type").remove()
      doc.head.getElementsByAttribute("charset").remove()
      doc.updateMetaCharsetElement(true)
      doc.charset(cs)
      doc.outputSettings(
        doc.outputSettings
          .escapeMode(Entities.EscapeMode.extended)
          .charset(cs)
          .prettyPrint(false)
      )
      doc
    }
}
