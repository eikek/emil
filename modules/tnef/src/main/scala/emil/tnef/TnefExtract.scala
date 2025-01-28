package emil.tnef

import java.io.InputStream

import cats.Applicative
import cats.effect._
import cats.implicits._
import emil._
import emil.tnef.Compat._
import fs2.{Chunk, Stream}
import org.apache.poi.hmef.HMEFMessage

object TnefExtract {

  /** Extracts the winmail.dat given as input stream file into a list of attachments. */
  def fromInputStream[F[_]: Applicative](in: InputStream): Vector[Attachment[F]] = {
    val message = new HMEFMessage(in)
    message
      .getAttachments()
      .toVector
      .map { a =>
        val bytes = a.getContents
        val cnt = Stream.chunk(Chunk.array(bytes))
        val name = Option(a.getFilename()).map(_.trim).filter(_.nonEmpty)
        Attachment(name, MimeType.octetStream, cnt, bytes.length.toLong.pure[F])
      }
  }

  /** Extracts the winmail.dat file given as a stream of bytes into a list of attachments. */
  def fromStream[F[_]: Async](
      data: Stream[F, Byte]
  ): Stream[F, Attachment[F]] =
    data
      .through(fs2.io.toInputStream)
      .flatMap(in => Stream.evalSeq(Sync[F].delay(fromInputStream[F](in))))

  /** Return the list of attachments if the given attachment is a tnef file, otherwise
    * return the input
    */
  def extractSingle[F[_]: Async](a: Attachment[F]): Stream[F, Attachment[F]] =
    if (TnefMimeType.matches(a.mimeType))
      fromStream[F](a.content)
    else
      Stream.emit(a)

  /** Go through the mail's attachments and replace each tnef attachment with its inner
    * attachments.
    */
  def replace[F[_]: Async](mail: Mail[F]): F[Mail[F]] = {
    val attachStream =
      for {
        as <- Stream.emits(mail.attachments.all)
        ext <- extractSingle[F](as)
      } yield ext

    attachStream.compile.toVector
      .map(as => mail.mapAttachments(_ => Attachments(as)))
  }

}
