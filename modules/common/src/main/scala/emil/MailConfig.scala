package emil

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{Duration, FiniteDuration}

import cats.implicits._
import emil.MailConfig.UrlParts

case class MailConfig(
    url: String,
    user: String,
    password: String,
    sslType: SSLType,
    disableCertificateCheck: Boolean = false,
    timeout: Duration = FiniteDuration(10, TimeUnit.SECONDS)
) {

  val urlParts: UrlParts =
    MailConfig.readUrlParts(url, sslType).fold(sys.error, identity)

  override def toString: String =
    s"MailConfig($url, $user, ***, $sslType, $timeout)"
}

object MailConfig {

  def gmailSmtp(email: String, pass: String): MailConfig =
    MailConfig("smtp://smtp.gmail.com:587", email, pass, SSLType.StartTLS)

  def gmailImap(email: String, pass: String): MailConfig =
    MailConfig("imaps://imap.gmail.com:993", email, pass, SSLType.SSL)

  case class UrlParts(protocol: String, host: String, port: Option[Int], path: String)

  private def readUrlParts(url: String, sslType: SSLType): Either[String, UrlParts] = {
    val regex = "([a-zA-Z]+)://([a-zA-z\\-\\.0-9]+)(:[0-9]+)?(.*)".r
    url match {
      case regex(proto, host, port, path) =>
        val protocol =
          if (sslType == SSLType.SSL && !proto.endsWith("s")) proto + "s"
          else proto
        Option(port)
          .map(_.substring(1).toInt.some)
          .map(s => Right(s))
          .getOrElse(Right(None))
          .map(op => UrlParts(protocol, host, op, path))
      case _ =>
        Left(s"Invalid url: $url")
    }
  }

}
