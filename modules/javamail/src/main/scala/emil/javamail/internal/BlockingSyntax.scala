package emil.javamail.internal

import cats.data.Kleisli
import cats.effect.{Blocker, ContextShift}
import emil._

object BlockingSyntax {
  implicit final class OperationOps[F[_], C <: Connection, A](op: Kleisli[F, C, A]) {
    def blockOn(blocker: Blocker)(implicit CS: ContextShift[F]): MailOp[F, C, A] =
      op.mapF(fa => blocker.blockOn(fa))
  }
}
