package emil.javamail.internal

import cats.effect.{Resource, Sync}
import emil.{Connection, MailConfig}
import jakarta.mail._

final case class JavaMailConnectionGeneric[
    +Str <: Store,
    +Trns <: Transport,
    +Fldr <: Folder
](
    config: MailConfig,
    session: Session,
    mailStore: Option[Str],
    mailTransport: Option[Trns]
) extends Connection {

  def store: Str =
    mailStore.getOrElse(sys.error(s"No store available for connection: ${config.url}"))

  def transport: Trns =
    mailTransport.getOrElse(
      sys.error(s"No transport available for connection: ${config.url}")
    )

  def folder[F[_]: Sync](
      name: String,
      mode: Int = Folder.READ_ONLY
  ): Resource[F, Fldr] =
    Resource
      .make(Sync[F].delay {
        val f      = store.getFolder(name).asInstanceOf[Fldr]
        val doOpen = f != null && !f.isOpen
        if (doOpen)
          f.open(mode)
        (f, doOpen)
      })(t =>
        Sync[F].delay {
          if (t._2 && t._1.isOpen)
            t._1.close(true)
        }
      )
      .map(_._1)
}
