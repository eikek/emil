package emil.javamail.internal

import cats.effect.{Resource, Sync}
import emil.{Connection, MailConfig}
import javax.mail.{Folder, Session, Store, Transport}

final case class JavaMailConnection(config: MailConfig, session: Session, mailStore: Option[Store]) extends Connection {

  def store: Store =
    mailStore.getOrElse(sys.error(s"No store available for connection: ${config.url}"))

  def transport[F[_]: Sync]: Resource[F, Transport] =
    Resource.make(Sync[F].delay {
      val t = session.getTransport
      t.connect()
      t
    })(trans => Sync[F].delay(trans.close()))

  def folder[F[_]: Sync](name: String, mode: Int = Folder.READ_ONLY): Resource[F, Folder] =
    Resource.make(Sync[F].delay {
      val f = store.getFolder(name)
      val doOpen = f != null && !f.isOpen
      if (doOpen) {
        f.open(mode)
      }
      (f, doOpen)
    })(t => Sync[F].delay({
      if (t._2 && t._1.isOpen) {
        t._1.close(true)
      }
    })).map(_._1)
}
