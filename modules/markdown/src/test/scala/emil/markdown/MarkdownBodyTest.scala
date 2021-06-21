package emil.markdown

import cats.effect._
import cats.effect.unsafe.implicits.global
import emil._
import emil.builder._
import minitest._

object MarkdownBodyTest extends SimpleTestSuite {

  test("set markdown body") {
    val md = "## Hello!\n\nThis is a *markdown* mail!"
    val mail: Mail[IO] = MailBuilder.build(
      From("me@test.com"),
      To("test@test.com"),
      Subject("Hello!"),
      MarkdownBody(md)
    )

    assertEquals(mail.body.textPart.unsafeRunSync().map(_.asString), Some(md))

    val expectedHtml = """<!DOCTYPE html>
        |<html>
        |<head>
        |<meta charset="utf-8"/>
        |<style>
        |body { font-size: 10pt; font-family: sans-serif; }
        |</style>
        |</head>
        |<body>
        |<h2>Hello!</h2>
        |<p>This is a <em>markdown</em> mail!</p>
        |</body>
        |</html>
        |""".stripMargin

    assertEquals(mail.body.htmlPart.unsafeRunSync().map(_.asString), Some(expectedHtml))
  }

}
