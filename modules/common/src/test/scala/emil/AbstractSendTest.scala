package emil

import cats.effect.IO
import cats.implicits._
import emil.builder._
import emil.test.GreenmailTestSuite

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
  }

  def findFirstMail[C <: Connection](a: Access[IO, C]): MailOp[IO, C, Mail[IO]] =
    a.getInbox
      .flatMap(in => a.search(in, 1)(SearchQuery.All))
      .map(_.mails.headOption.toRight(new Exception("No mail found.")))
      .mapF(_.rethrow)
      .flatMap(a.loadMail)
      .map(_.toRight(new Exception("Mail could not be loaded.")))
      .mapF(_.rethrow)

}
