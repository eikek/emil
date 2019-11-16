package emil

import cats.data.Kleisli
import cats.effect.Sync
import cats.{Applicative, ApplicativeError}

trait Ops {

  type MailOp[F[_], C <: Connection, A] = Kleisli[F, C, A]

  object MailOp {
    def apply[F[_], C <: Connection, A](run: C => F[A]): MailOp[F, C, A] =
      Kleisli(run)

    def of[F[_]: Sync, C <: Connection, A](run: C => A): MailOp[F, C, A] =
      MailOp(conn => Sync[F].delay(run(conn)))

    def error[F[_], C <: Connection, A](
        msg: String
    )(implicit ev: ApplicativeError[F, Throwable]): MailOp[F, C, A] =
      MailOp(_ => ApplicativeError[F, Throwable].raiseError(new Exception(msg)))

    def pure[F[_]: Applicative, C <: Connection, A](value: A): MailOp[F, C, A] =
      Kleisli.pure(value)
  }

}
