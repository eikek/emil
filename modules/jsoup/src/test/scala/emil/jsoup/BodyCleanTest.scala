package emil.jsoup

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import cats.effect._
import cats.effect.unsafe.implicits.global
import emil._
import emil.builder._
import munit._
import org.jsoup._

class BodyCleanTest extends FunSuite {

  test("see how jsoup works") {
    val htmlText =
      """<html>
        |<head>
        |<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        |<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        |<meta name="format-detection" content="telephone=no">
        |<meta http-equiv="X-UA-Compatible" content="IE=edge">
        |<meta charset="utf-8"/>
        |</head>
        |<body><p>f&uuml;r dich</p></body>
        |</html>""".stripMargin

    assertEquals(
      Jsoup
        .parse(
          new ByteArrayInputStream(htmlText.getBytes(StandardCharsets.ISO_8859_1)),
          null, // null charset should use one from header
          ""
        )
        .charset,
      StandardCharsets.UTF_8
    )

    val doc = Jsoup.parse(htmlText)
    doc.head.getElementsByAttributeValue("http-equiv", "content-type").remove()
    doc.head.getElementsByAttribute("charset").remove()
    assert(!doc.outerHtml.contains("utf-8"))
    doc.updateMetaCharsetElement(true)
    doc.charset(StandardCharsets.ISO_8859_1)
    assert(doc.outerHtml.contains("ISO-8859-1"))
  }

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
      cleanMail.body.htmlPart.map(_.map(_.asString)).unsafeRunSync()
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
      cleanMail.body.textPart.map(_.map(_.asString)).unsafeRunSync()
    val expect = """Hello World!"""
    assertEquals(str, Some(expect))
  }

  test("fix charset") {
    val htmlMail =
      """<html><head><meta charset="iso-8859-1"/></head>
        |<body><h1>Brief f&uuml;r Sie</h2><p script="alert('hi!');">Gr&uuml;&szlig;e – LG.</p></body>
        |</html>""".stripMargin.replace("\n", "")

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
      cleanMail.body.htmlPart.map(_.map(_.asString)).unsafeRunSync()
    val expect = """<html><head><meta charset="UTF-8"></head>
                   |<body>
                   |<h1>Brief für Sie</h1>
                   |<p>Grüße – LG.</p>
                   |</body>
                   |</html>""".stripMargin.replace("\n", "")
    assertEquals(str, Some(expect))
  }
}
