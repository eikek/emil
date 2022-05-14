package emil.test

import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration._

import com.icegreen.greenmail.imap.ImapHostManager
import com.icegreen.greenmail.util.{GreenMail, ServerSetup}
import emil.{MailAddress, MailConfig, SSLType}
import org.slf4j.LoggerFactory

class GreenmailServer(imapPort: Int, smtpPort: Int, users: List[MailAddress]) {

  private[this] val logger = LoggerFactory.getLogger(getClass)
  private val started: AtomicBoolean = new AtomicBoolean(false)
  private val greenMail = {
    val imap = new ServerSetup(imapPort, "localhost", ServerSetup.PROTOCOL_IMAP)
    val smtp = new ServerSetup(smtpPort, "localhost", ServerSetup.PROTOCOL_SMTP)
    val gm = new GreenMail(Array(imap, smtp))
    users.foreach { user =>
      gm.getManagers.getUserManager.createUser(user.address, user.address, user.address)
    }
    gm
  }

  def getManagers =
    greenMail.getManagers()

  def start(): Unit =
    if (started.compareAndSet(false, true)) {
      logger.info(
        "Starting local greenmail mail servers: imap:{} smtp:{}",
        imapPort,
        smtpPort
      )
      greenMail.start()
    } else
      logger.warn("GreenMail already started. Check your test code.")

  def stop(): Unit = {
    logger.info(
      "Stopping local greenmail mail servers: imap:{} smtp:{}",
      imapPort,
      smtpPort
    )
    greenMail.stop()
  }

  def smtpConfig(user: MailAddress): MailConfig =
    MailConfig(
      s"smtp://localhost:$smtpPort",
      user.address,
      user.address,
      SSLType.NoEncryption
    )

  def smtpConfigNoUser: MailConfig =
    MailConfig(s"smtp://localhost:$smtpPort", "", "", SSLType.NoEncryption)

  def imapConfig(user: MailAddress): MailConfig =
    MailConfig(
      s"imap://localhost:$imapPort",
      user.address,
      user.address,
      SSLType.NoEncryption
    )

  def waitForReceive(mails: Int, timeout: FiniteDuration = 10.seconds): Unit =
    assert(
      greenMail.waitForIncomingEmail(timeout.toMillis, mails),
      "Timeout reached while waiting for mails"
    )

  def removeAllMails(): Unit =
    greenMail.purgeEmailFromAllMailboxes()

  def imapManager: ImapHostManager =
    greenMail.getManagers.getImapHostManager

}

object GreenmailServer {

  def randomPorts(users: MailAddress*): GreenmailServer =
    new GreenmailServer(randomPort(10), randomPort(10), users.toList)

  private def randomPort(tries: Int): Int = {
    if (tries == 0)
      throw new IllegalStateException("Cannot obtain an unused port.");
    val random = new SecureRandom;
    val port = random.nextInt(20000) + 1024;
    try {
      val socket = new Socket(null: String, port)
      socket.close()
      randomPort(tries - 1);
    } catch {
      case _: Exception => port;
    }
  }

}
