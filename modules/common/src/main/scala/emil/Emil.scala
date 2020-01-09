package emil

import cats.effect.{Bracket, Resource}
import cats.data.NonEmptyList

trait Emil[F[_]] { self =>

  type C <: Connection

  def connection(mc: MailConfig): Resource[F, C]

  def sender: Send[F, C]

  def access: Access[F, C]

  def apply(mc: MailConfig)(implicit F: Bracket[F, Throwable]): Emil.Run[F, C] =
    new Emil.Run[F, C] {

      def run[A](op: MailOp[F, C, A]): F[A] =
        self.connection(mc).use(op.run)

      def send(mail: Mail[F], mails: Mail[F]*): F[NonEmptyList[String]] =
        run(sender.sendMails(NonEmptyList.of(mail, mails: _*)))
    }
}

object Emil {

  trait Run[F[_], C <: Connection] {

    def run[A](op: MailOp[F, C, A]): F[A]

    def send(mail: Mail[F], mails: Mail[F]*): F[NonEmptyList[String]]
  }
}
