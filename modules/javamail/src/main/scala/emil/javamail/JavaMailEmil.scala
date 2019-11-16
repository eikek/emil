package emil.javamail

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.Properties

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import cats.implicits._
import emil._
import emil.javamail.conv.{Conv, MessageIdEncode, MsgConv}
import emil.javamail.internal._
import javax.mail.Session
import javax.mail.internet.MimeMessage

final class JavaMailEmil[F[_]: Sync: ContextShift](blocker: Blocker)
    extends Emil[F, JavaMailConnection] {

  def connection(mc: MailConfig): Resource[F, JavaMailConnection] =
    ConnectionResource[F](mc)

  def sender: Send[F, JavaMailConnection] =
    new SendImpl[F](blocker)

  def access: Access[F, JavaMailConnection] =
    new AccessImpl[F](blocker)
}

object JavaMailEmil {

  def apply[F[_]: Sync: ContextShift](blocker: Blocker): Emil[F, JavaMailConnection] =
    new JavaMailEmil[F](blocker)

  def mailToString[F[_]: Sync](
      mail: Mail[F]
  )(implicit cm: MsgConv[Mail[F], F[MimeMessage]]): F[String] = {
    val session = Session.getInstance(new Properties())
    cm.convert(session, MessageIdEncode.Given, mail)
      .map(
        msg =>
          ThreadClassLoader {
            val out = new ByteArrayOutputStream()
            msg.writeTo(out)
            out.toString(StandardCharsets.UTF_8.name())
          }
      )
  }

  def mailFromString[F[_]: Sync](str: String)(implicit cm: Conv[MimeMessage, Mail[F]]): F[Mail[F]] =
    Sync[F].delay {
      val session = Session.getInstance(new Properties())
      ThreadClassLoader {
        val msg =
          new MimeMessage(session, new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)))
        cm.convert(msg)
      }
    }
}
