package emil.javamail

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.Properties

import cats.effect._
import cats.implicits._
import emil._
import emil.javamail.conv.{Conv, MessageIdEncode, MsgConv}
import emil.javamail.internal._
import fs2.{Chunk, Stream}
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import scodec.bits.ByteVector

final class JavaMailEmil[F[_]: Sync] private (
    settings: Settings
) extends Emil[F] {
  GlobalProperties.unsafeApplySystemProperties()

  type C = JavaMailConnection

  def connection(mc: MailConfig): Resource[F, JavaMailConnection] =
    ConnectionResource[F](mc, settings)

  def sender: Send[F, JavaMailConnection] =
    new SendImpl[F]

  def access: Access[F, JavaMailConnection] =
    new AccessGimapImpl[F]
}

object JavaMailEmil {
  GlobalProperties.unsafeApplySystemProperties()

  def apply[F[_]: Sync](settings: Settings = Settings.defaultSettings): Emil[F] =
    new JavaMailEmil[F](settings)

  def mailToString[F[_]: Sync](
      mail: Mail[F]
  )(implicit cm: MsgConv[Mail[F], F[MimeMessage]]): F[String] =
    mailToByteArray[F](mail).map(b => new String(b, StandardCharsets.UTF_8))

  def mailToByteVector[F[_]: Sync](
      mail: Mail[F]
  )(implicit cm: MsgConv[Mail[F], F[MimeMessage]]): F[ByteVector] =
    mailToByteArray[F](mail).map(ByteVector.view)

  /** Creates a new allocated byte array containing the give mail. */
  def mailToByteArray[F[_]: Sync](
      mail: Mail[F]
  )(implicit cm: MsgConv[Mail[F], F[MimeMessage]]): F[Array[Byte]] =
    ThreadClassLoader {
      val session = Session.getInstance(new Properties())
      cm.convert(session, MessageIdEncode.GivenOrRandom, mail)
        .map(msg =>
          ThreadClassLoader {
            val out = new ByteArrayOutputStream()
            msg.writeTo(out)
            out.toByteArray
          }
        )
    }

  def mailToByteStream[F[_]: Sync](
      mail: Mail[F]
  )(implicit cm: MsgConv[Mail[F], F[MimeMessage]]): Stream[F, Byte] =
    Stream
      .eval(mailToByteVector[F](mail))
      .flatMap(bs => Stream.chunk(Chunk.byteVector(bs)))

  def mailFromString[F[_]: Sync](
      str: String
  )(implicit cm: Conv[MimeMessage, Mail[F]]): F[Mail[F]] =
    mailFromByteArray[F](str.getBytes(StandardCharsets.UTF_8))

  def mailFromByteVector[F[_]: Sync](
      bytes: ByteVector
  )(implicit cm: Conv[MimeMessage, Mail[F]]): F[Mail[F]] =
    mailFromByteArray(bytes.toArray)

  def mailFromByteArray[F[_]: Sync](
      bytes: Array[Byte]
  )(implicit cm: Conv[MimeMessage, Mail[F]]): F[Mail[F]] =
    Sync[F].delay {
      ThreadClassLoader {
        val session = Session.getInstance(new Properties())
        val msg =
          new MimeMessage(
            session,
            new ByteArrayInputStream(bytes)
          )
        cm.convert(msg)
      }
    }
}
