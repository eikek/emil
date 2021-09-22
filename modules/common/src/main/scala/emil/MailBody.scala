package emil

import cats.Applicative
import cats.implicits._

sealed trait MailBody[F[_]] {

  def withText(text: F[BodyContent]): MailBody[F]

  def withHtml(html: F[BodyContent]): MailBody[F]

  def fold[A](
      empty: MailBody.Empty[F] => A,
      text: MailBody.Text[F] => A,
      html: MailBody.Html[F] => A,
      both: MailBody.HtmlAndText[F] => A
  ): A

  /** Return the html or the text content. If only text is available, it is applied to the
    * given function that may convert it into html.
    */
  def htmlContent(
      txtToHtml: BodyContent => BodyContent
  )(implicit ev: Applicative[F]): F[BodyContent] =
    fold(
      _ => BodyContent.empty.pure[F],
      txt => txt.text.map(txtToHtml),
      html => html.html,
      both => both.html
    )

  /** Return only the text part if present. */
  def textPart(implicit ev: Applicative[F]): F[Option[BodyContent]] =
    fold(
      _ => (None: Option[BodyContent]).pure[F],
      txt => txt.text.map(_.some),
      _ => (None: Option[BodyContent]).pure[F],
      both => both.text.map(_.some)
    )

  /** Return only the html part if present. */
  def htmlPart(implicit ev: Applicative[F]): F[Option[BodyContent]] =
    fold(
      _ => (None: Option[BodyContent]).pure[F],
      _ => (None: Option[BodyContent]).pure[F],
      html => html.html.map(_.some),
      both => both.html.map(_.some)
    )

  def isEmpty: Boolean =
    fold(_ => true, _ => false, _ => false, _ => false)

  def nonEmpty: Boolean =
    !isEmpty
}

object MailBody {

  final case class Empty[F[_]]() extends MailBody[F] {
    def withText(text: F[BodyContent]): MailBody[F] = Text(text)

    def withHtml(html: F[BodyContent]): MailBody[F] = Html(html)

    def fold[A](
        empty: MailBody.Empty[F] => A,
        text: MailBody.Text[F] => A,
        html: MailBody.Html[F] => A,
        both: MailBody.HtmlAndText[F] => A
    ): A = empty(this)

  }

  final case class Text[F[_]](text: F[BodyContent]) extends MailBody[F] {
    def withText(other: F[BodyContent]): MailBody[F] =
      Text(other)

    def withHtml(html: F[BodyContent]): MailBody[F] =
      HtmlAndText(text, html)

    def modify(f: BodyContent => BodyContent)(implicit F: Applicative[F]): Text[F] =
      Text(text.map(f))

    def fold[A](
        empty: MailBody.Empty[F] => A,
        text: MailBody.Text[F] => A,
        html: MailBody.Html[F] => A,
        both: MailBody.HtmlAndText[F] => A
    ): A = text(this)
  }

  final case class Html[F[_]](html: F[BodyContent]) extends MailBody[F] {
    def withText(text: F[BodyContent]): MailBody[F] =
      HtmlAndText(text, html)

    def withHtml(other: F[BodyContent]): MailBody[F] =
      Html(other)

    def modify(f: BodyContent => BodyContent)(implicit F: Applicative[F]): Html[F] =
      Html(html.map(f))

    def fold[A](
        empty: MailBody.Empty[F] => A,
        txt: MailBody.Text[F] => A,
        h: MailBody.Html[F] => A,
        both: MailBody.HtmlAndText[F] => A
    ): A = h(this)
  }

  final case class HtmlAndText[F[_]](text: F[BodyContent], html: F[BodyContent])
      extends MailBody[F] {
    def withText(text: F[BodyContent]): MailBody[F] =
      HtmlAndText(text, html)

    def withHtml(html: F[BodyContent]): MailBody[F] =
      HtmlAndText(text, html)

    def modify(ft: BodyContent => BodyContent, fh: BodyContent => BodyContent)(implicit
        F: Applicative[F]
    ): HtmlAndText[F] =
      HtmlAndText(text.map(ft), html.map(fh))

    def fold[A](
        empty: MailBody.Empty[F] => A,
        txt: MailBody.Text[F] => A,
        h: MailBody.Html[F] => A,
        both: MailBody.HtmlAndText[F] => A
    ): A = both(this)
  }

  def empty[F[_]]: MailBody[F] =
    Empty()

  def text[F[_]: Applicative](text: BodyContent): MailBody[F] =
    if (text.isEmpty) empty[F] else Text(text.pure[F])

  def html[F[_]: Applicative](html: BodyContent): MailBody[F] =
    Html(html.pure[F])

  def both[F[_]: Applicative](text: BodyContent, html: BodyContent) =
    HtmlAndText(text.pure[F], html.pure[F])
}
