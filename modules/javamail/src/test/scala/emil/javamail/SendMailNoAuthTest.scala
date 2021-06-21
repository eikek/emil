package emil.javamail

import cats.effect._
import cats.effect.unsafe.implicits.global
import emil._
import emil.builder._
import emil.test.GreenmailTestSuite

class SendMailNoAuthTest extends GreenmailTestSuite {

  def users = Nil

  lazy val emil: Emil[IO] = JavaMailEmil[IO]()

  test("Send mail without auth") {
    context.server.getManagers.getUserManager.setAuthRequired(false)
    assertEquals(false, context.server.getManagers.getUserManager.isAuthRequired)
    val htmlBody = "<h1>Hello!</h1>\n<p>This <b>is</b> a mail.</p>"
    val mail: Mail[IO] = MailBuilder.build(
      From("me@test.com"),
      To("you@test.com"),
      Subject("Hello!"),
      UserAgent("my-email-client"),
      TextBody("Hello!\n\nThis is a mail."),
      HtmlBody(htmlBody)
    )

    emil(smtpConfNoUser).send(mail).unsafeRunSync()
    server.waitForReceive(1)
  }
}
