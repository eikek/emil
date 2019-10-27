package emil.javamail.conv

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import emil._
import emil.javamail.conv.BodyDecode.{Body, BodyAttach}
import emil.javamail.internal.{ThreadClassLoader, Using, Util}
import fs2.Chunk.ByteVectorChunk
import fs2.Stream
import javax.mail.internet.{MimeMessage, MimeUtility}
import javax.mail.{Header => _, _}
import scodec.bits.ByteVector

import scala.io.Source
import emil.javamail.internal.EnumerationConverter._

/** Read a recursive multipart message into our simplified structure:
 *
 * - the first text, html or alternative part is defining MailBody
 * - all other parts are flattened into a list of attachments
 */
trait BodyDecode {

  implicit def attachmentDecode[F[_]: Sync]: Conv[BodyPart, Attachments[F]] =
    Conv({ bp =>
      if (bp.isMimeType("multipart/*")) {
        val mp = bp.getContent.asInstanceOf[Multipart]
        (0 until mp.getCount).
          toVector.
          map(mp.getBodyPart).
          map(attachmentDecode[F].convert).
          foldLeft(Attachments.empty[F])(_ ++ _)
      } else {
        val mt = MimeTypeDecode.parse(bp.getContentType).
          toOption.getOrElse(MimeType.octetStream)
        val data = bp.getDataHandler.getInputStream
        val filename = Option(bp.getFileName)
        val (len, dataAsStream) = BodyDecode.loadInputStream(data)
        Attachments(Attachment(filename, mt, dataAsStream, len.pure[F]))
      }
    })


  implicit def mailBodyDecode[F[_]: Sync]
  (implicit ca: Conv[BodyPart, Attachments[F]]): Conv[MimeMessage, BodyAttach[F]] =
    Conv({ msg =>
      Util.withReadFolder(msg) { _ =>
        msg.getContent match {
          case str: String =>
            val body =
              if (msg.isMimeType(MimeType.textHtml.asString)) Body(text = None, html = Option(str))
              else Body(text = Option(str), html = None)

             BodyAttach(body, Attachments.empty[F])

          case mp: Multipart =>
            if (BodyDecode.isAlternative(mp)) {
              BodyAttach(BodyDecode.getAlternativeBody(Body(None, None), mp), Attachments.empty[F])
            } else {
              (0 until mp.getCount).
                map(mp.getBodyPart).
                foldLeft(BodyAttach[F](Body(None, None), Attachments.empty[F])) { (result, part) =>
                  if (BodyDecode.maySetTextBody(result.body.text, MimeType.textPlain, part)) {
                    result.copy(body = result.body.copy(text = BodyDecode.getTextContent(part).some))
                  } else if (BodyDecode.maySetTextBody(result.body.html, MimeType.textHtml, part)) {
                    result.copy(body = result.body.copy(html = BodyDecode.getTextContent(part).some))
                  } else if (part.isMimeType("multipart/alternative")) {
                    result.copy(body = BodyDecode.getAlternativeBody(result.body, part.getContent.asInstanceOf[Multipart]))
                  } else {
                    result.copy(attachments = result.attachments ++ ca.convert(part))
                  }
                }
            }

          case _ =>
            sys.error(s"Not implemented content: ${msg.getContent}")
        }
      }
    })

  implicit def mailDecode[F[_]: Sync]
  (implicit cb: Conv[MimeMessage, BodyAttach[F]], ch: Conv[MimeMessage, MailHeader]): Conv[MimeMessage, Mail[F]] =
    Conv({ msg => ThreadClassLoader {
      Util.withReadFolder(msg) { _ =>
        val additionalHeaders = msg.getNonMatchingHeaders(MailHeader.headerNames.toArray).asScalaList.
          foldLeft(Headers.empty) { (hds, h) =>
            hds.add(Header(MimeUtility.decodeText(h.getName), MimeUtility.decodeText(h.getValue)))
          }
        val bodyAttach = cb.convert(msg)
        val mailHeader = ch.convert(msg)
        emil.Mail(mailHeader, additionalHeaders, bodyAttach.body.toMailBody[F], bodyAttach.attachments)
      }
    }})
}

object BodyDecode {
  private[this] val moreCharsets = Map(
    "win1250" -> "windows-1250",
    "win1252" -> "windows-1252"
  )
  final case class Body(text: Option[String], html: Option[String]) {
    def toMailBody[F[_]: Applicative]: MailBody[F] =
      (text, html) match {
        case (Some(txt), Some(h)) => MailBody.both(txt, h)
        case (Some(txt), None) => MailBody.text(txt)
        case (None, Some(h)) => MailBody.html(h)
        case (None, None) => MailBody.text("")
      }
  }

  final case class BodyAttach[F[_]](body: Body, attachments: Attachments[F])

  private def getAlternativeBody(body: Body, alt: Multipart): Body = {
    (0 until alt.getCount).
      map(alt.getBodyPart).
      foldLeft(body) { (body, part) =>
        if (maySetTextBody(body.text, MimeType.textPlain, part)) {
          body.copy(text = getTextContent(part).some)
        } else if (maySetTextBody(body.html, MimeType.textHtml, part)) {
          body.copy(html = getTextContent(part).some)
        } else {
          body
        }
      }
  }

  private def getTextContent(p: BodyPart): String = {
    // Try to map some known incorrect names to correct ones
    // and then try to lookup this charset while falling back
    // to utf-8. `(String) part.getContent()` would throw an exception.
    val mt = MimeTypeDecode.parse(p.getContentType).
      toOption.getOrElse(MimeType.octetStream)
    val charset = mt.params.get("charset").
      map(cs => moreCharsets.getOrElse(cs.toLowerCase, cs)).
      flatMap(cs => Either.catchNonFatal(Charset.forName(cs)).toOption).
      getOrElse(StandardCharsets.UTF_8)

    Using.resource(p.getInputStream) { in =>
      Source.fromInputStream(in, charset.name()).mkString
    }
  }

  private def maySetTextBody(current: Option[String], mimetype: MimeType, part: BodyPart) =
    current.isEmpty && part.isMimeType(mimetype.asString) &&
      Option(part.getDisposition).forall(s => s.equalsIgnoreCase(Part.INLINE))

  private def isAlternative(mp: Multipart) =
    Option(mp.getContentType).
      flatMap(ct => MimeTypeDecode.parse(ct).toOption).
      exists(mt => mt.sub.equalsIgnoreCase("alternative"))

  private def loadInputStream[F[_]: Sync](in: InputStream): (Long, Stream[F, Byte]) = {
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
}
