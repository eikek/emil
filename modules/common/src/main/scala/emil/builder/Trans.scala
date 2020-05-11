package emil.builder

import emil._
import cats._

trait Trans[F[_]] { self =>
  def apply(mail: Mail[F]): Mail[F]

  def andThen(next: Trans[F]): Trans[F] =
    Trans(m => next(self(m)))
}

object Trans {
  def id[F[_]]: Trans[F] = Trans(identity)

  def apply[F[_]](f: Mail[F] => Mail[F]): Trans[F] =
    (m: Mail[F]) => f(m)

  implicit def transMonoid[F[_]]: Monoid[Trans[F]] =
    Monoid.instance(id[F], _ andThen _)

  def combineAll[F[_]](ts: Seq[Trans[F]]): Trans[F] =
    Monoid[Trans[F]].combineAll(ts)
}
