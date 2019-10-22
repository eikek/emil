package emil

import cats.effect.{Bracket, Resource}

trait Emil[F[_], C <: Connection] { self =>

  def connection(mc: MailConfig): Resource[F, C]

  def sender: Send[F, C]

  def access: Access[F, C]

  def apply(mc: MailConfig)(implicit F: Bracket[F, Throwable]): Emil.Run[F, C] =
    new Emil.Run[F, C] {
      def run[A](op: MailOp[F, C, A]): F[A] =
        self.connection(mc).use(op.run)

      def send(mails: Mail[F]*): F[Unit] =
        run(sender.sendMails(mails))
    }
}

object Emil {

  trait Run[F[_], C <: Connection] {
    def run[A](op: MailOp[F, C, A]): F[A]

    def send(mails: Mail[F]*): F[Unit]
  }
}
