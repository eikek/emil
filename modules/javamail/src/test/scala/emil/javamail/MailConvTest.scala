package emil.javamail

import cats.effect.IO
import emil._
import emil.builder._
import emil.javamail.syntax._
import minitest._

object MailConvTest extends SimpleTestSuite {

  test("write text mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      TextBody("This is text")
    )

    val str = mail.serialize.unsafeRunSync().
      replaceAll("Date:.*", "Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)")
    assertEquals(str, """Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)
                        |From: test@test.com
                        |To: test@test.com
                        |Subject: Hello
                        |MIME-Version: 1.0
                        |Content-Type: text/plain; charset=us-ascii
                        |Content-Transfer-Encoding: 7bit
                        |
                        |This is text""".stripMargin.replace("\n", "\r\n"))
  }

  test("write html mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      HtmlBody("<p>This is html</p>")
    )

    val str = mail.serialize.unsafeRunSync().
      replaceAll("Date:.*", "Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)")
    assertEquals(str, """Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)
                        |From: test@test.com
                        |To: test@test.com
                        |Subject: Hello
                        |MIME-Version: 1.0
                        |Content-Type: text/html; charset=us-ascii
                        |Content-Transfer-Encoding: 7bit
                        |
                        |<p>This is html</p>""".stripMargin.replace("\n", "\r\n"))
  }

  test("write empty mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
    )

    val str = mail.serialize.unsafeRunSync().
      replaceAll("Date:.*", "Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)")
    assertEquals(str, """Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)
                        |From: test@test.com
                        |To: test@test.com
                        |Subject: Hello
                        |MIME-Version: 1.0
                        |Content-Type: text/plain; charset=us-ascii
                        |Content-Transfer-Encoding: 7bit
                        |
                        |""".stripMargin.replace("\n", "\r\n"))
  }

  test("write alternative mail") {
    val mail = MailBuilder.build[IO](
      From("test@test.com"),
      To("test@test.com"),
      Subject("Hello"),
      HtmlBody("<p>This is html</p>"),
      TextBody("This is html as text")
    )

    val str = mail.serialize.unsafeRunSync().
      replaceAll("Date:.*", "Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)")

    val partChars = str.lines.
      find(_.contains("boundary")).
      map(_.trim.drop(10).dropRight(1)).
      getOrElse(sys.error("no part boundary found"))

    val expected =  s"""Date: Sun, 27 Oct 2019 10:15:36 +0100 (CET)
                       |From: test@test.com
                       |To: test@test.com
                       |Subject: Hello
                       |MIME-Version: 1.0
                       |Content-Type: multipart/alternative;[]
                       |{}boundary="$partChars"
                       |
                       |--$partChars
                       |Content-Type: text/plain; charset=us-ascii
                       |Content-Transfer-Encoding: 7bit
                       |
                       |This is html as text
                       |--$partChars
                       |Content-Type: text/html; charset=us-ascii
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
    val mail2 = Mail.deserialize[IO](str).unsafeRunSync().
      copy(additionalHeaders = Headers.empty).
      mapMailHeader(_.copy(id = "").copy(sentDate = None))
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
    val mail2 = Mail.deserialize[IO](str).unsafeRunSync().
      copy(additionalHeaders = Headers.empty).
      mapMailHeader(_.copy(id = "").copy(sentDate = None))
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
    val mail2 = Mail.deserialize[IO](str).unsafeRunSync().
      copy(additionalHeaders = Headers.empty).
      mapMailHeader(_.copy(id = "").copy(sentDate = None))
    assertEquals(mail, mail2)
  }
}
