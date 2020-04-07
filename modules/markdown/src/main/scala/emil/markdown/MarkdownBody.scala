package emil.markdown

import cats.implicits._
import cats.Applicative
import cats.effect._
import fs2.Stream
import emil._
import emil.builder.Trans

/** A transformation function to use with `emil.builder' package that
  * takes a plain text string and converts it into a html mail body
  * using markdown.
  */
case class MarkdownBody[F[_]: Applicative](
    md: F[String],
    cfg: MarkdownConfig = MarkdownConfig.defaultConfig
) extends Trans[F] {

  def apply(mail: Mail[F]): Mail[F] = {
    val html = md.map(s => BodyContent(internal.Markdown.toHtml(s, cfg)))
    mail.mapBody(_ => MailBody.HtmlAndText(md.map(BodyContent.apply), html))
  }

  def withConfig(config: MarkdownConfig): MarkdownBody[F] =
    copy(cfg = config)
}

object MarkdownBody {

  def fromBytes[F[_]: Sync](bytes: Stream[F, Byte]): MarkdownBody[F] =
    fromUtf8String(bytes.through(fs2.text.utf8Decode))

  def fromUtf8String[F[_]: Sync](str: Stream[F, String]): MarkdownBody[F] =
    MarkdownBody(str.foldMonoid.compile.lastOrError)

  def apply[F[_]: Applicative](md: String): MarkdownBody[F] =
    MarkdownBody(md.pure[F])

  def makeHtml(cfg: MarkdownConfig)(text: String): String =
    internal.Markdown.toHtml(text, cfg)
}
