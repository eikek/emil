package emil.javamail

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.Properties

import cats.effect._
import cats.implicits._
import emil._
import emil.javamail.conv.{Conv, MessageIdEncode, MsgConv}
import emil.javamail.internal._
import javax.mail.Session
import javax.mail.internet.MimeMessage
import scodec.bits.ByteVector

final class JavaMailEmil[F[_]: Sync: ContextShift] private (
    blocker: Blocker,
    settings: Settings
) extends Emil[F] {
  GlobalProperties.applySystemProperties[IO].unsafeRunSync()

  type C = JavaMailConnection

  def connection(mc: MailConfig): Resource[F, JavaMailConnection] =
    ConnectionResource[F](mc, settings)

  def sender: Send[F, JavaMailConnection] =
    new SendImpl[F](blocker)

  def access: Access[F, JavaMailConnection] =
    new AccessImpl[F](blocker)
}

object JavaMailEmil {
  GlobalProperties.applySystemProperties[IO].unsafeRunSync()

  def apply[F[_]: Sync: ContextShift](
      blocker: Blocker,
      settings: Settings = Settings.defaultSettings
  ): Emil[F] =
    new JavaMailEmil[F](blocker, settings)

  def mailToString[F[_]: Sync](
      mail: Mail[F]
  )(implicit cm: MsgConv[Mail[F], F[MimeMessage]]): F[String] =
    ThreadClassLoader {
      val session = Session.getInstance(new Properties())
      cm.convert(session, MessageIdEncode.GivenOrRandom, mail)
        .map(msg =>
          ThreadClassLoader {
            val out = new ByteArrayOutputStream()
            msg.writeTo(out)
            out.toString(StandardCharsets.UTF_8.name())
          }
        )
    }

  def mailFromString[F[_]: Sync](
      str: String
  )(implicit cm: Conv[MimeMessage, Mail[F]]): F[Mail[F]] =
    Sync[F].delay {
      ThreadClassLoader {
        val session = Session.getInstance(new Properties())
        val msg =
          new MimeMessage(
            session,
            new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))
          )
        cm.convert(msg)
      }
    }

  def mailFromByteArray[F[_]: Sync](
      bytes: Array[Byte]
  )(implicit cm: Conv[MimeMessage, Mail[F]]): F[Mail[F]] =
    Sync[F].delay {
      val session = Session.getInstance(new Properties())
      val msg =
        new MimeMessage(
          session,
          new ByteArrayInputStream(bytes)
        )
      cm.convert(msg)
    }

  def mailFromByteVector[F[_]: Sync](
      bytes: ByteVector
  )(implicit cm: Conv[MimeMessage, Mail[F]]): F[Mail[F]] =
    mailFromByteArray(bytes.toArray)
}
