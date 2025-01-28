package emil

import cats.data.NonEmptyList
import cats.effect.MonadCancel
import cats.effect.Resource

trait Emil[F[_]] { self =>

  type C <: Connection

  def connection(mc: MailConfig): Resource[F, C]

  def sender: Send[F, C]

  def access: Access[F, C]

  def apply(mc: MailConfig)(implicit F: MonadCancel[F, Throwable]): Emil.Run[F, C] =
    new Emil.Run[F, C] {

      def run[A](op: MailOp[F, C, A]): F[A] =
        self.connection(mc).use(op.run)

      def send_(mails: NonEmptyList[Mail[F]]): F[NonEmptyList[String]] =
        run(sender.sendMails(mails))
    }
}

object Emil {

  trait Run[F[_], C <: Connection] {

    def run[A](op: MailOp[F, C, A]): F[A]

    def send(mail: Mail[F], mails: Mail[F]*): F[NonEmptyList[String]] =
      send_(NonEmptyList(mail, mails.toList))

    def send_(mails: NonEmptyList[Mail[F]]): F[NonEmptyList[String]]
  }
}
