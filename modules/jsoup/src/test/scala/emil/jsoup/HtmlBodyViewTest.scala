package emil.jsoup

import scala.concurrent.ExecutionContext

import cats.effect._
import emil._
import emil.builder._
import emil.javamail.syntax._
import minitest._

object HtmlBodyViewTest extends SimpleTestSuite {
  implicit val CS = IO.contextShift(scala.concurrent.ExecutionContext.global)
  val blocker     = Blocker.liftExecutionContext(ExecutionContext.global)

  test("create view") {
    val htmlMail = """<h1>A header</h2><p script="alert('hi!');">Hello</p><p>World<p>"""

    val mail: Mail[IO] = MailBuilder.build(
      From("Me Jones", "me@test.com"),
      To("test@test.com"),
      Subject("Hello!"),
      HtmlBody(htmlMail)
    )

    val htmlView = HtmlBodyView(
      mail.body,
      Some(mail.header)
    )
    val str =
      htmlView.map(_.asString).unsafeRunSync()
    assert(!str.contains("alert"))
    assert(
      str.contains("<strong>From:</strong> <code>Me Jones &lt;me@test.com&gt;</code><br>")
    )
    assert(str.contains("<strong>To:</strong> <code>test@test.com</code><br>"))
    assert(str.contains("<strong>Subject:</strong> <code>Hello!</code><br>"))
    assert(str.contains("<strong>Date:</strong> <code>-</code>"))
  }

  test("create from iso transferred utf8 html") {
    val url  = getClass.getResource("/mails/html-utf8-as-iso.eml")
    val mail = Mail.fromURL[IO](url, blocker).unsafeRunSync()
    val htmlView = HtmlBodyView(
      mail.body,
      Some(mail.header)
    )

    val str = htmlView.unsafeRunSync().asString
    assert(str.contains("in Kürze"))
    assert(
      str.contains(
        "<strong>From:</strong> <code>Service &lt;service@test.com&gt;</code><br>"
      )
    )
    assert(str.contains("<strong>To:</strong> <code>xyz@test.com</code><br>"))
    assert(
      str.contains(
        "<strong>Subject:</strong> <code>Deine Bestellung wurde versandt - Rechnung &amp; Sendungsverfolgung</code><br>"
      )
    )
    assert(
      str.contains("<strong>Date:</strong> <code>Mon, 11 May 2020 11:07:54 GMT</code>")
    )
  }

  test("html-escape plain text bodies by default") {
    val mail: Mail[IO] = MailBuilder.build(
      From("Me Jones", "me@test.com"),
      To("test@test.com"),
      Subject("Hello!"),
      TextBody("Hello <script> & such.")
    )

    val htmlView = HtmlBodyView(
      mail.body,
      Some(mail.header)
    )
    val str =
      htmlView.map(_.asString).unsafeRunSync()
    assert(str.contains("<p>Hello &lt;script&gt; &amp; such.</p>"))
    assert(!str.contains("<script>"))
  }
}
