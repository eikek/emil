package emil.javamail.internal

import java.security.NoSuchProviderException
import java.util.Properties

import scala.concurrent.duration.Duration

import cats.effect.{Resource, Sync}
import cats.implicits._
import emil.javamail.Settings
import emil.{MailConfig, SSLType}
import jakarta.mail._

object ConnectionResource {
  private[this] val logger = Logger(getClass)

  def apply[F[_]: Sync](
      mc: MailConfig,
      settings: Settings
  ): Resource[F, JavaMailConnection] =
    Resource.make(make(mc, settings))(conn =>
      Sync[F].blocking {
        conn.mailStore.foreach(_.close())
        conn.mailTransport.foreach(_.close())
      }
    )

  def make[F[_]: Sync](mc: MailConfig, settings: Settings): F[JavaMailConnection] =
    Sync[F].blocking {
      val session = createSession(mc, settings)
      val protocol = mc.urlParts.protocol.toLowerCase
      if (protocol.startsWith("imap") || protocol.startsWith("gimap")) {
        val store = createImapStore(session, mc)
        JavaMailConnectionGeneric(mc, session, Some(store), None)
      } else {
        val tp = createSmtpTransport(session, mc)
        JavaMailConnectionGeneric(mc, session, None, Some(tp))
      }
    }

  private def createImapStore(session: Session, mc: MailConfig): Store = {
    val store = session.getStore(mc.urlParts.protocol)
    if (mc.user.nonEmpty) {
      logger.debug(s"Connect store with $mc")
      store.connect(mc.user, mc.password)
    } else
      store.connect()
    store
  }

  private def createSmtpTransport(session: Session, mc: MailConfig): Transport = {
    val tp = session.getTransport(mc.urlParts.protocol)
    if (tp == null)
      throw new NoSuchProviderException(
        s"Transport cannot be created for protocol: ${mc.urlParts.protocol}"
      )
    if (mc.user.nonEmpty) {
      logger.debug(s"Connect transport with $mc")
      tp.connect(mc.user, mc.password)
    } else
      tp.connect()
    tp
  }

  private def createSession(mc: MailConfig, settings: Settings): Session = {
    val host = mc.urlParts.host
    val port = mc.urlParts.port
    val proto = mc.urlParts.protocol

    val props = new Properties

    if (mc.user.nonEmpty) {
      props.put(s"mail.$proto.auth", "true");
      // JavaMail has XOAUTH2 default disabled. And it is the last
      // mechanism tried. When connecting to GMail, this won't work,
      // because first LOGIN is tried which fails to authenticate. So
      // XOAUTH must be first. This is why
      // "mail.smtp.auth.xoauth2.disable -> false" is not working for
      // gmail.
      val mechanisms = "LOGIN PLAIN DIGEST-MD5 NTLM"
      if (mc.enableXOAuth2) {
        props.put(s"mail.$proto.auth.mechanisms", "XOAUTH2 " + mechanisms)
      } else {
        props.put(s"mail.$proto.auth.mechanisms", mechanisms)
      }

      props.put(s"mail.$proto.user", mc.user)
    }

    if (settings.debug) {
      props.put("mail.debug", "true")
    }

    props.put(s"mail.$proto.host", host)
    port.foreach(p => props.put(s"mail.$proto.port", Integer.toString(p)))
    if (mc.timeout.isFinite && mc.timeout > Duration.Zero) {
      val value = mc.timeout.toMillis.toString
      props.put(s"mail.$proto.connectiontimeout", value)
      props.put(s"mail.$proto.timeout", value)
      props.put(s"mail.$proto.writetimeout", value)
    }

    mc.sslType match {
      case SSLType.SSL =>
        props.put(s"mail.$proto.ssl.enable", "true")
        if (mc.disableCertificateCheck)
          props.put(s"mail.$proto.ssl.trust", "*")
      case SSLType.StartTLS =>
        props.put(s"mail.$proto.starttls.enable", "true")
        props.put(s"mail.$proto.starttls.required", "true")
        if (mc.disableCertificateCheck)
          props.put(s"mail.$proto.ssl.trust", "*")
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

    // let users of this library override everything here
    settings.props(proto).foreach { case (k, v) => props.put(k, v) }

    if (mc.user.nonEmpty) {
      logger.debug(s"Creating session with authenticator and props: $props")
      Session.getInstance(
        props,
        new Authenticator {
          override def getPasswordAuthentication: PasswordAuthentication =
            new PasswordAuthentication(mc.user, mc.password)
        }
      )
    } else {
      logger.debug(s"Creating session without authenticator and props: $props")
      Session.getInstance(props)
    }
  }
}
