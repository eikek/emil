package emil.jsoup

import minitest._
import cats.effect._
import emil._
import emil.builder._
import org.jsoup._

object HtmlBodyViewTest extends SimpleTestSuite {

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
      Some(mail.header),
      None, //here the emil-markdown module can be used to convert text->html
      Some(BodyClean.whitelistClean(EmailWhitelist.default))
    )
    val str =
      htmlView.map(_.asString).unsafeRunSync
    val expect =
      """<html>
        | <head>
        |  <meta charset="UTF-8">
        | </head>
        | <body>
        |  <div style="padding-bottom: 0.8em;"> <strong>From:</strong> <code>Me Jones &lt;me@test.com&gt;</code>
        |   <br> <strong>To:</strong> <code>test@test.com</code>
        |   <br> <strong>Subject:</strong> <code>Hello!</code>
        |  </div>
        |  <h1>A header</h1>
        |  <p>Hello</p>
        |  <p>World</p>
        |  <p></p>
        | </body>
        |</html>""".stripMargin
    assertEquals(Jsoup.parse(str).outerHtml, Jsoup.parse(expect).outerHtml)
  }

}
