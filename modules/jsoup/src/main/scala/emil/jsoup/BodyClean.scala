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

/** Modifies/cleans html bodies.
  */
case class BodyClean[F[_]: Applicative](change: Document => Document) extends Trans[F] {

  def apply(mail: Mail[F]): Mail[F] =
    mail.mapBody(BodyClean.modifyBody(change))

}

object BodyClean {

  def apply[F[_]: Applicative](whitelist: Whitelist): Trans[F] =
    BodyClean[F](whitelistClean(whitelist) _)

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
    def changeDoc(doc: Document): Document = {
      doc.charset(html.charsetOrUtf8)
      change(doc)
    }

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

  def whitelistClean(whitelist: Whitelist)(doc: Document): Document = {
    val cleaner = new Cleaner(whitelist)
    val body    = cleaner.clean(doc).body
    doc.body.replaceWith(body)
    doc
  }
}
