package emil

import cats.{Applicative, Functor}
import cats.implicits._

sealed trait MailBody[F[_]] {

  def withText(text: F[String]): MailBody[F]

  def withHtml(html: F[String]): MailBody[F]

  def fold[A]( text: MailBody.Text[F] => A
             , html: MailBody.Html[F] => A
             , both: MailBody.HtmlAndText[F] => A): A

  def htmlContent(txtToHtml: String => String)(implicit ev: Functor[F]): F[String] =
    fold(txt => txt.text.map(txtToHtml)
      , html => html.html
      , both => both.html)
}

object MailBody {

  final case class Text[F[_]](text: F[String]) extends MailBody[F] {
    def withText(other: F[String]): MailBody[F] =
      Text(other)

    def withHtml(html: F[String]): MailBody[F] =
      HtmlAndText(text, html)

    def fold[A](text: MailBody.Text[F] => A, html: MailBody.Html[F] => A, both: MailBody.HtmlAndText[F] => A): A =
      text(this)
  }

  final case class Html[F[_]](html: F[String]) extends MailBody[F] {
    def withText(text: F[String]): MailBody[F] =
      HtmlAndText(text, html)

    def withHtml(other: F[String]): MailBody[F] =
      Html(other)

    def fold[A](txt: MailBody.Text[F] => A, h: MailBody.Html[F] => A, both: MailBody.HtmlAndText[F] => A): A =
      h(this)
  }

  final case class HtmlAndText[F[_]](text: F[String], html: F[String]) extends MailBody[F] {
    def withText(text: F[String]): MailBody[F] =
      HtmlAndText(text, html)

    def withHtml(html: F[String]): MailBody[F] =
      HtmlAndText(text, html)

    def fold[A](txt: MailBody.Text[F] => A, h: MailBody.Html[F] => A, both: MailBody.HtmlAndText[F] => A): A =
      both(this)
  }

  def text[F[_]: Applicative](text: String): MailBody[F] =
    Text(text.pure[F])

  def html[F[_]: Applicative](html: String): MailBody[F] =
    Html(html.pure[F])

  def both[F[_]: Applicative](text: String, html: String) =
    HtmlAndText(text.pure[F], html.pure[F])
}
