package emil

import cats.data.Kleisli
import cats.effect.Sync
import cats.{Applicative, ApplicativeError}

trait Ops {

  type MailOp[F[_], C, A] = Kleisli[F, C, A]

  object MailOp {
    def apply[F[_], C, A](run: C => F[A]): MailOp[F, C, A] =
      Kleisli(run)

    def of[F[_]: Sync, C, A](run: C => A): MailOp[F, C, A] =
      MailOp(conn => Sync[F].delay(run(conn)))

    def error[F[_], C, A](
        msg: String
    )(implicit e: ApplicativeError[F, Throwable]): MailOp[F, C, A] =
      MailOp(_ => e.raiseError(new Exception(msg)))

    def pure[F[_]: Applicative, C, A](value: A): MailOp[F, C, A] =
      Kleisli.pure(value)
  }

}
