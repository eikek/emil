package emil.javamail.conv

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.charset.Charset
import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import emil._
import emil.javamail.conv.BodyDecode.{Body, BodyAttach}
import emil.javamail.internal.EnumerationConverter._
import emil.javamail.internal.{ThreadClassLoader, Using, Util}
import fs2.Chunk.ByteVectorChunk
import fs2.Stream
import jakarta.mail.internet.{MimeMessage, MimeUtility}
import jakarta.mail.{Header => _, _}
import scodec.bits.ByteVector

/** Read a recursive multipart message into our simplified structure:
  *
  * - the first text, html or alternative part is defining MailBody
  * - all other parts are flattened into a list of attachments
  */
trait BodyDecode {

  implicit def attachmentDecode[F[_]: Sync]: Conv[Part, Attachments[F]] =
    Conv { bp =>
      if (bp.isMimeType("multipart/*")) {
        val mp = bp.getContent.asInstanceOf[Multipart]
        (0 until mp.getCount).toVector
          .map(mp.getBodyPart)
          .map(attachmentDecode[F].convert)
          .foldLeft(Attachments.empty[F])(_ ++ _)
      } else {
        val mt =
          MimeTypeDecode.parse(bp.getContentType).toOption.getOrElse(MimeType.octetStream)
        val data                = bp.getDataHandler.getInputStream
        val filename            = Option(bp.getFileName)
        val (len, dataAsStream) = BodyDecode.loadInputStream(data)
        Attachments(Attachment(filename, mt, dataAsStream, len.pure[F]))
      }
    }

  implicit def mailBodyDecode[F[_]: Sync](implicit
      ca: Conv[Part, Attachments[F]]
  ): Conv[MimeMessage, BodyAttach[F]] =
    Conv(msg => Util.withReadFolder(msg)(_ => decodePart(msg, ca, BodyAttach.empty[F])))

  private def decodePart[F[_]: Applicative](
      p: Part,
      ca: Conv[Part, Attachments[F]],
      result: BodyAttach[F]
  ): BodyAttach[F] =
    if (p.isMimeType("text/*")) {
      val cnt = BodyDecode.getTextContent(p)
      val body =
        if (p.isMimeType(MimeType.textHtml.asString))
          Body(text = None, html = Option(cnt))
        else Body(text = Option(cnt), html = None)

      result.copy(body = body)
    } else if (p.isMimeType("multipart/*")) {
      val mp = p.getContent.asInstanceOf[Multipart]
      if (BodyDecode.isAlternative(mp)) {
        val ba = BodyDecode.getAlternativeBody[F](BodyAttach.empty[F], mp)(ca)
        result.merge(ba)
      } else
        (0 until mp.getCount)
          .map(mp.getBodyPart)
          .foldLeft(BodyAttach.empty[F]) { (acc, part) =>
            if (
              BodyDecode.maySetTextBody(
                acc.body.text,
                MimeType.textPlain,
                part
              )
            )
              acc.copy(
                body = acc.body.copy(text = BodyDecode.getTextContent(part).some)
              )
            else if (
              BodyDecode
                .maySetTextBody(acc.body.html, MimeType.textHtml, part)
            )
              acc.copy(
                body = acc.body.copy(html = BodyDecode.getTextContent(part).some)
              )
            else if (part.isMimeType("multipart/alternative") && acc.body.isEmpty) {
              val mp = part.getContent.asInstanceOf[Multipart]
              val ba = BodyDecode.getAlternativeBody(acc, mp)(ca)
              result.merge(ba)
            } else {
              val next = decodePart(part, ca, result)
              acc.merge(next)
            }
          }

    } else
      result.copy(attachments = result.attachments ++ ca.convert(p))

  implicit def mailDecode[F[_]: Sync](implicit
      cb: Conv[MimeMessage, BodyAttach[F]],
      ch: Conv[MimeMessage, MailHeader]
  ): Conv[MimeMessage, Mail[F]] =
    Conv { msg =>
      ThreadClassLoader {
        Util.withReadFolder(msg) { _ =>
          val additionalHeaders = msg
            .getNonMatchingHeaders(MailHeader.headerNames.toArray)
            .asScalaList
            .foldLeft(Headers.empty) { (hds, h) =>
              hds.add(
                Header(
                  MimeUtility.decodeText(h.getName),
                  MimeUtility.decodeText(h.getValue)
                )
              )
            }
          val bodyAttach = cb.convert(msg)
          val mailHeader = ch.convert(msg)
          emil.Mail(
            mailHeader,
            additionalHeaders,
            bodyAttach.body.toMailBody[F],
            bodyAttach.attachments
          )
        }
      }
    }

  implicit lazy val mailDecodeRaw: Conv[MimeMessage, ByteVector] =
    Conv { msg =>
      ThreadClassLoader {
        Util.withReadFolder(msg) { _ =>
          val out = new ByteArrayOutputStream()
          try {
            msg.writeTo(out)
            ByteVector.view(out.toByteArray)
          }
          finally out.close()
        }
      }
    }

}

object BodyDecode {
  private[this] val moreCharsets = Map(
    "win1250" -> "windows-1250",
    "win1252" -> "windows-1252"
  )
  final case class Body(text: Option[BodyContent], html: Option[BodyContent]) {
    def toMailBody[F[_]: Applicative]: MailBody[F] =
      (text, html) match {
        case (Some(txt), Some(h)) => MailBody.both(txt, h)
        case (Some(txt), None)    => MailBody.text(txt)
        case (None, Some(h))      => MailBody.html(h)
        case (None, None)         => MailBody.empty
      }
    def isEmpty: Boolean =
      text.isEmpty && html.isEmpty
  }

  final case class BodyAttach[F[_]](body: Body, attachments: Attachments[F]) {
    def modifyBody(f: Body => Body): BodyAttach[F] =
      copy(body = f(body))

    def merge(other: BodyAttach[F])(implicit F: Applicative[F]): BodyAttach[F] =
      if (body.isEmpty)
        BodyAttach(other.body, attachments ++ other.attachments)
      else if (other.body.isEmpty)
        BodyAttach(body, attachments ++ other.attachments)
      else {
        val html  = other.body.html.map(c => Attachment.content[F](c, MimeType.textHtml))
        val plain = other.body.text.map(c => Attachment.content[F](c, MimeType.textPlain))
        val all   = Attachments(html.toVector ++ plain.toVector)
        BodyAttach(
          body,
          attachments ++ other.attachments ++ all
        )
      }
  }
  object BodyAttach {
    def empty[F[_]] = BodyAttach[F](Body(None, None), Attachments.empty[F])
  }

  private def getAlternativeBody[F[_]](cnt: BodyAttach[F], alt: Multipart)(implicit
      ca: Conv[Part, Attachments[F]]
  ): BodyAttach[F] =
    (0 until alt.getCount).map(alt.getBodyPart).foldLeft(cnt) { (result, part) =>
      if (maySetTextBody(result.body.text, MimeType.textPlain, part))
        result.modifyBody(body => body.copy(text = getTextContent(part).some))
      else if (maySetTextBody(result.body.html, MimeType.textHtml, part))
        result.modifyBody(body => body.copy(html = getTextContent(part).some))
      else if (part.isMimeType("multipart/*")) {
        val mp = part.getContent.asInstanceOf[Multipart]
        getAlternativeBody(result, mp)
      } else
        result.copy(attachments = result.attachments ++ ca.convert(part))
    }

  private def getTextContent(p: Part): BodyContent = {
    // Try to map some known incorrect names to correct ones
    // and then try to lookup this charset while falling back
    // to utf-8. `(String) part.getContent()` would throw an exception.
    val mt =
      MimeTypeDecode.parse(p.getContentType).toOption.getOrElse(MimeType.octetStream)
    val charset = mt.params
      .get("charset")
      .map(cs => moreCharsets.getOrElse(cs.toLowerCase, cs))
      .flatMap(cs => Either.catchNonFatal(Charset.forName(cs)).toOption)

    val bv = Using.resource(p.getInputStream)(loadBytes)
    BodyContent(bv, charset)
  }

  private def maySetTextBody(
      current: Option[BodyContent],
      mimetype: MimeType,
      part: BodyPart
  ) =
    current.isEmpty && part.isMimeType(mimetype.asString) &&
      Option(part.getDisposition).forall(s => s.equalsIgnoreCase(Part.INLINE))

  private def isAlternative(mp: Multipart) =
    Option(mp.getContentType)
      .flatMap(ct => MimeTypeDecode.parse(ct).toOption)
      .exists(mt => mt.sub.equalsIgnoreCase("alternative"))

  private def loadInputStream[F[_]](in: InputStream): (Long, Stream[F, Byte]) = {
    val buffer = Array.ofDim[Byte](16 * 1024)
    @annotation.tailrec
    def go(chunks: Vector[ByteVectorChunk], len: Long): (Long, Vector[ByteVectorChunk]) =
      in.read(buffer) match {
        case -1 => (len, chunks)
        case n =>
          val ch = ByteVectorChunk(ByteVector(buffer, 0, n))
          go(chunks :+ ch, len + n)
      }

    val (len, bv) = go(Vector.empty, 0)
    in.close()

    val empty: Stream[F, Byte] = Stream.empty.covary[F]
    (len, bv.map(Stream.chunk).map(_.covary[F]).foldLeft(empty)(_ ++ _))
  }

  private def loadBytes(in: InputStream): ByteVector = {
    val baos   = new ByteArrayOutputStream()
    val buffer = Array.ofDim[Byte](8 * 1024)

    @scala.annotation.tailrec
    def go(): Unit = {
      val len = in.read(buffer)
      if (len <= 0) ()
      else {
        baos.write(buffer, 0, len)
        go()
      }
    }

    go()
    ByteVector.view(baos.toByteArray)
  }
}
