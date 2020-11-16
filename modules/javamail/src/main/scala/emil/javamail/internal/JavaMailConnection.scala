package emil.javamail.internal

import jakarta.mail._

import cats.effect.{Resource, Sync}
import emil.{Connection, MailConfig}

final case class JavaMailConnection(
    config: MailConfig,
    session: Session,
    mailStore: Option[Store],
    mailTransport: Option[Transport]
) extends Connection {

  def store: Store =
    mailStore.getOrElse(sys.error(s"No store available for connection: ${config.url}"))

  def transport: Transport =
    mailTransport.getOrElse(
      sys.error(s"No transport available for connection: ${config.url}")
    )

  def folder[F[_]: Sync](
      name: String,
      mode: Int = Folder.READ_ONLY
  ): Resource[F, Folder] =
    Resource
      .make(Sync[F].delay {
        val f      = store.getFolder(name)
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
