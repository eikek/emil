package emil.javamail

import cats.effect._
import emil._
import emil.builder._
import emil.javamail.syntax._
import minitest._
import scala.concurrent.ExecutionContext

object MailConvTest extends SimpleTestSuite {
  implicit val CS = IO.contextShift(scala.concurrent.ExecutionContext.global)
  val blocker = Blocker.liftExecutionContext(ExecutionContext.global)

  test("write text mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      TextBody("This is text"),
      MessageID("<bla>")
    )

    val str = mail.serialize
      .unsafeRunSync()
      .replaceAll("Date:.*", "Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)")
    assertEquals(
      str,
      """Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)
                        |From: test@test.com
                        |To: test@test.com
                        |Message-ID: <bla>
                        |Subject: Hello
                        |MIME-Version: 1.0
                        |Content-Type: text/plain; charset=utf-8
                        |Content-Transfer-Encoding: 7bit
                        |
                        |This is text""".stripMargin.replace("\n", "\r\n")
    )
  }

  test("write html mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      HtmlBody("<p>This is html</p>"),
      MessageID("<bla>")
    )

    val str = mail.serialize
      .unsafeRunSync()
      .replaceAll("Date:.*", "Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)")
    assertEquals(
      str,
      """Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)
                        |From: test@test.com
                        |To: test@test.com
                        |Message-ID: <bla>
                        |Subject: Hello
                        |MIME-Version: 1.0
                        |Content-Type: text/html; charset=utf-8
                        |Content-Transfer-Encoding: 7bit
                        |
                        |<p>This is html</p>""".stripMargin.replace("\n", "\r\n")
    )
  }

  test("write empty mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      MessageID("<bla>")
    )

    val str = mail.serialize
      .unsafeRunSync()
      .replaceAll("Date:.*", "Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)")
    assertEquals(
      str,
      """Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)
                        |From: test@test.com
                        |To: test@test.com
                        |Message-ID: <bla>
                        |Subject: Hello
                        |MIME-Version: 1.0
                        |Content-Type: text/plain; charset=utf-8
                        |Content-Transfer-Encoding: 7bit
                        |
                        |""".stripMargin.replace("\n", "\r\n")
    )
  }

  test("write alternative mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      HtmlBody("<p>This is html</p>"),
      TextBody("This is html as text"),
      MessageID("<bla>")
    )

    val str = mail.serialize
      .unsafeRunSync()
      .replaceAll("Date:.*", "Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)")

    val partChars = str.lines
      .find(_.contains("boundary"))
      .map(_.trim.drop(10).dropRight(1))
      .getOrElse(sys.error("no part boundary found"))

    val expected = s"""Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)
                       |From: test@test.com
                       |To: test@test.com
                       |Message-ID: <bla>
                       |Subject: Hello
                       |MIME-Version: 1.0
                       |Content-Type: multipart/alternative;[]
                       |{}boundary="$partChars"
                       |
                       |--$partChars
                       |Content-Type: text/plain; charset=utf-8
                       |Content-Transfer-Encoding: 7bit
                       |
                       |This is html as text
                       |--$partChars
                       |Content-Type: text/html; charset=utf-8
                       |Content-Transfer-Encoding: 7bit
                       |
                       |<p>This is html</p>
                       |--$partChars--
                       |""".stripMargin.replace("\n", "\r\n").replace("{}", "\t").replace("[]", " ")

    assertEquals(str, expected)
  }

  test("read text mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      TextBody("This is text")
    )

    val str = mail.serialize.unsafeRunSync()
    val mail2 = Mail
      .deserialize[IO](str)
      .unsafeRunSync()
      .copy(additionalHeaders = Headers.empty)
      .mapMailHeader(_.copy(id = "").copy(originationDate = None).copy(messageId = None))
    assertEquals(mail, mail2)
  }

  test("read html mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      HtmlBody("This is text")
    )

    val str = mail.serialize.unsafeRunSync()
    val mail2 = Mail
      .deserialize[IO](str)
      .unsafeRunSync()
      .copy(additionalHeaders = Headers.empty)
      .mapMailHeader(_.copy(id = "").copy(originationDate = None).copy(messageId = None))
    assertEquals(mail, mail2)
  }

  test("read alternative mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      TextBody("This is text"),
      HtmlBody("This is html/text")
    )

    val str = mail.serialize.unsafeRunSync()
    val mail2 = Mail
      .deserialize[IO](str)
      .unsafeRunSync()
      .copy(additionalHeaders = Headers.empty)
      .mapMailHeader(_.copy(id = "").copy(originationDate = None).copy(messageId = None))
    assertEquals(mail, mail2)
  }

  test("read test mail 1") {
    val url = getClass.getResource("/mails/test.eml")
    val mail = Mail.fromURL[IO](url, blocker).unsafeRunSync
    assertEquals(mail.header.received.size, 7)
    assertEquals(mail.attachments.size, 1)
    assert(mail.body.nonEmpty)
  }

  test("read test mail 2") {
    val url = getClass.getResource("/mails/test2.eml")
    val mail = Mail.fromURL[IO](url, blocker).unsafeRunSync
    assertEquals(mail.header.received.size, 3)
    assertEquals(mail.attachments.size, 0)
    assert(mail.body.nonEmpty)
  }

  test("read alternative mail") {
    val url = getClass.getResource("/mails/alt.eml")
    val mail = Mail.fromURL[IO](url, blocker).unsafeRunSync
    assertEquals(mail.attachments.size, 0)
    assert(mail.body.nonEmpty)
    assert(mail.body.textPart.unsafeRunSync.isDefined)
    assert(mail.body.htmlPart.unsafeRunSync.isDefined)
    assertEquals(mail.header.received.size, 6)
  }
}
