package emil.test

import emil.test.GreenmailTestSuite.Context
import emil.{MailAddress, MailConfig}
import munit._

abstract class GreenmailTestSuite extends FunSuite {

  var context: Context = null

  def users: List[MailAddress]

  override def beforeEach(ctx: BeforeEach): Unit = {
    context = GreenmailTestSuite.createContext(users)
    context.server.start()
  }

  override def afterEach(ctx: AfterEach): Unit =
    if (context != null) {
      context.server.stop()
      context = null
    }

  def server: GreenmailServer =
    context.server

  def smtpConf(user: MailAddress): MailConfig =
    server.smtpConfig(user)

  def smtpConfNoUser: MailConfig =
    server.smtpConfigNoUser

  def imapConf(user: MailAddress): MailConfig =
    server.imapConfig(user)

}

object GreenmailTestSuite {

  case class Context(server: GreenmailServer)

  def createContext(users: List[MailAddress]): Context =
    Context(GreenmailServer.randomPorts(users))
}
