package emil.javamail

import cats.effect._
import minitest._
import emil.test.GreenmailTestSuite
import emil.builder._
import emil._

object SendMailNoAuthTest extends GreenmailTestSuite[Unit] {

  def users                     = Nil
  def setup(): Unit             = ()
  def tearDown(env: Unit): Unit = ()

  lazy val emil: Emil[IO] = JavaMailEmil[IO](blocker)

  test("Send mail without auth") { _ =>
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
