package emil.javamail.internal

import java.util.Properties

import cats.effect.{Resource, Sync}
import cats.implicits._
import emil.{MailConfig, SSLType}
import javax.mail.{Authenticator, PasswordAuthentication, Session, Store}

import scala.concurrent.duration.Duration

object ConnectionResource {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](mc: MailConfig, debug: Boolean = false): Resource[F, JavaMailConnection] =
    Resource.make(make(mc, debug))(
      conn =>
        conn.mailStore match {
          case Some(s) => Sync[F].delay(s.close())
          case None    => ().pure[F]
        }
    )

  def make[F[_]: Sync](mc: MailConfig, debug: Boolean = false): F[JavaMailConnection] =
    Sync[F].delay {
      val session = createSession(mc, debug)
      if (mc.urlParts.protocol.toLowerCase.startsWith("imap")) {
        val store = createImapStore(session, mc)
        JavaMailConnection(mc, session, Some(store))
      } else {
        JavaMailConnection(mc, session, None)
      }
    }

  private def createImapStore(session: Session, mc: MailConfig): Store = {
    val store = session.getStore(mc.urlParts.protocol)
    if (mc.user.nonEmpty) {
      store.connect(mc.user, mc.password)
    } else {
      store.connect()
    }
    store
  }

  private def createSession(mc: MailConfig, debug: Boolean): Session = {
    val host = mc.urlParts.host
    val port = mc.urlParts.port
    val proto = mc.urlParts.protocol

    val props = new Properties()
    if (mc.user.nonEmpty) {
      props.put(s"mail.$proto.user", mc.user)
    }
    if (debug) {
      props.put("mail.debug", "true")
    }

    props.put(s"mail.$proto.host", host)
    port.foreach { p =>
      props.put(s"mail.$proto.port", Integer.toString(p))
    }
    if (mc.timeout.isFinite && mc.timeout > Duration.Zero) {
      val value = mc.timeout.toMillis.toString
      props.put(s"mail.$proto.connectiontimeout", value)
      props.put(s"mail.$proto.timeout", value)
      props.put(s"mail.$proto.writetimeout", value)
    }

    mc.sslType match {
      case SSLType.SSL =>
        props.put(s"mail.$proto.ssl.enable", "true")
        if (mc.disableCertificateCheck) {
          props.put(s"mail.$proto.ssl.trust", "*")
        }
      case SSLType.StartTLS =>
        props.put(s"mail.$proto.starttls.enable", "true")
        props.put(s"mail.$proto.starttls.required", "true")
        if (mc.disableCertificateCheck) {
          props.put(s"mail.$proto.ssl.trust", "*")
        }
      case SSLType.NoEncryption =>
        props.remove(s"mail.$proto.ssl.enable")
        props.remove(s"mail.$proto.starttls.required")
        props.remove(s"mail.$proto.starttls.enable")
    }

    // see https://stackoverflow.com/questions/2043792/missing-start-boundary-exception-when-reading-messages-with-an-attachment-file
    // the parse method fails inside MimeMessage otherwise
    props.put("mail.mime.parameters.strict", "false")
    props.put("mail.mime.multipart.ignoreexistingboundaryparameter", "true")
    props.put("mail.mime.multipart.ignoremissingboundaryparameter", "true")

    if (mc.user.nonEmpty) {
      logger.trace(s"Creating session with authenticator and props: $props")
      Session.getInstance(props, new Authenticator() {
        override def getPasswordAuthentication: PasswordAuthentication =
          new PasswordAuthentication(mc.user, mc.password)
      })
    } else {
      logger.trace(s"Creating session without authenticator and props: $props")
      Session.getInstance(props)
    }
  }
}
