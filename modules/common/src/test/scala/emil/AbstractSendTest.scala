package emil

import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.implicits._
import emil.builder._
import emil.test.GreenmailTestSuite
import scodec.bits.ByteVector

abstract class AbstractSendTest[A] extends GreenmailTestSuite[A] {
  val emil: Emil[IO]

  val user1 = MailAddress.unsafe(None, "joe@test.com")
  val user2 = MailAddress.unsafe(None, "joan@test.com")

  def users: List[MailAddress] = List(user1, user2)

  test("Send and receive mail") { _ =>
    val htmlBody = "<h1>Hello!</h1>\n<p>This <b>is</b> a mail.</p>"
    val mail: Mail[IO] = MailBuilder.build(
      From(user1),
      To(user2),
      Subject("Hello!"),
      UserAgent("my-email-client"),
      TextBody("Hello!\n\nThis is a mail."),
      HtmlBody(htmlBody),
      Attach(Attachment.textPlain[IO]("hello world!")).withFilename("test.txt")
    )

    emil(smtpConf(user1)).send(mail).unsafeRunSync()
    server.waitForReceive(1)

    val mail2 = emil(imapConf(user2)).run(findFirstMail(emil.access)).unsafeRunSync()
    assertEquals(mail.header.subject, mail2.header.subject)
    assertEquals(mail.attachments.size, mail2.attachments.size)
    assertEquals(mail.body.htmlContent(identity).unsafeRunSync().asString, htmlBody)
    assert(mail2.body.fold(_ => false, _ => false, _ => false, _ => true))
    assertEquals(
      mail2.body.htmlContent(identity).unsafeRunSync().asString.replace("\r\n", "\n"),
      htmlBody
    )
    assertEquals(
      mail2.additionalHeaders.find("user-agent"),
      Some(Header("User-Agent", "my-email-client"))
    )

    val mail2raw = emil(imapConf(user2))
      .run(findFirstMailRaw(emil.access))
      .unsafeRunSync()
    val actual = new String(mail2raw.toArray, StandardCharsets.UTF_8)
    val expected =
      """Return-Path: <joe@test.com>
        |Received: from ... (HELO ...); Thu Feb  :: CET 
        |Date: Thu,  Feb  :: + (CET)
        |From: joe@test.com
        |To: joan@test.com
        |Message-ID: <..@[...]>
        |Subject: Hello!
        |MIME-Version: .
        |Content-Type: multipart/mixed; 
        |	boundary="----=_Part__."
        |User-Agent: my-email-client
        |
        |------=_Part_1_272299100.1642183145458
        |Content-Type: multipart/alternative; 
        |	boundary="----=_Part_0_2060779434.1642183145447"
        |
        |------=_Part_0_2060779434.1642183145447
        |Content-Type: text/plain; charset=utf-8
        |Content-Transfer-Encoding: 7bit
        |
        |Hello!
        |
        |This is a mail.
        |------=_Part_0_2060779434.1642183145447
        |Content-Type: text/html; charset=utf-8
        |Content-Transfer-Encoding: 7bit
        |
        |<h1>Hello!</h1>
        |<p>This <b>is</b> a mail.</p>
        |------=_Part_0_2060779434.1642183145447--
        |
        |------=_Part_1_272299100.1642183145458
        |Content-Type: text/plain; charset=UTF-8
        |Content-Transfer-Encoding: 7bit
        |Content-Disposition: attachment; filename=test.txt
        |Content-Description: attachment
        |
        |hello world!
        |------=_Part_1_272299100.1642183145458--""".stripMargin
    def normalize(s: String): String =
      s.split("\n")
        .filterNot(x => x.startsWith("Received") || x.startsWith("Date"))
        .mkString("\n")
        .replaceAll("\\d+", "")
        .replaceAll("\\s+", "")
        .replace("\r\n", "\n")
        .stripTrailing()
        .stripLeading()
    val actualNormalized   = normalize(actual)
    val expectedNormalized = normalize(expected)
    assertEquals(actualNormalized, expectedNormalized)
  }

  def findFirstMail[C <: Connection](a: Access[IO, C]): MailOp[IO, C, Mail[IO]] =
    a.getInbox
      .flatMap(in => a.search(in, 1)(SearchQuery.All))
      .map(_.mails.headOption.toRight(new Exception("No mail found.")))
      .mapF(_.rethrow)
      .flatMap(a.loadMail)
      .map(_.toRight(new Exception("Mail could not be loaded.")))
      .mapF(_.rethrow)

  def findFirstMailRaw[C <: Connection](a: Access[IO, C]): MailOp[IO, C, ByteVector] =
    a.getInbox
      .flatMap(in => a.search(in, 1)(SearchQuery.All))
      .map(_.mails.headOption.toRight(new Exception("No mail found.")))
      .mapF(_.rethrow)
      .flatMap(a.loadMailRaw)
      .map(_.toRight(new Exception("Mail could not be loaded.")))
      .mapF(_.rethrow)

}
