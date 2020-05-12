package emil.jsoup

import minitest._
import cats.effect._
import emil._
import emil.builder._

object BodyCleanTest extends SimpleTestSuite {

  test("clean body") {
    val htmlMail = """<h1>A header</h2><p script="alert('hi!');">Hello</p><p>World<p>"""

    val mail: Mail[IO] = MailBuilder.build(
      From("me@test.com"),
      To("test@test.com"),
      Subject("Hello!"),
      HtmlBody(htmlMail)
    )

    val cleanMail = mail.asBuilder
      .add(BodyClean(EmailWhitelist.default))
      .build

    val str =
      cleanMail.body.htmlPart.map(_.map(_.asString)).unsafeRunSync
    val expect = """<html><head><meta charset="UTF-8"></head>
        |<body>
        |<h1>A header</h1>
        |<p>Hello</p>
        |<p>World</p>
        |<p></p>
        |</body>
        |</html>""".stripMargin.replace("\n", "")
    assertEquals(str, Some(expect))
  }

  test("ignore text only") {
    val textBody = """Hello World!"""

    val mail: Mail[IO] = MailBuilder.build(
      From("me@test.com"),
      To("test@test.com"),
      Subject("Hello!"),
      TextBody(textBody)
    )

    val cleanMail = mail.asBuilder
      .add(BodyClean(EmailWhitelist.default))
      .build

    val str =
      cleanMail.body.textPart.map(_.map(_.asString)).unsafeRunSync
    val expect = """Hello World!"""
    assertEquals(str, Some(expect))
  }

  test("produce ascii output") {
    val htmlMail = """<h1>Brief für Sie</h2><p script="alert('hi!');">Grüße – LG.</p>"""

    val mail: Mail[IO] = MailBuilder.build(
      From("me@test.com"),
      To("test@test.com"),
      Subject("Hello!"),
      HtmlBody(htmlMail)
    )

    val cleanMail = mail.asBuilder
      .add(BodyClean(EmailWhitelist.default))
      .build

    val str =
      cleanMail.body.htmlPart.map(_.map(_.asString)).unsafeRunSync
    val expect = """<html><head><meta charset="UTF-8"></head>
        |<body>
        |<h1>Brief f&uuml;r Sie</h1>
        |<p>Gr&uuml;&szlig;e &ndash; LG.</p>
        |</body>
        |</html>""".stripMargin.replace("\n", "")
    assertEquals(str, Some(expect))
  }
}
