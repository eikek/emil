package emil

import cats.syntax.all._
import cats.{Eq, Hash}

final case class SearchResult[A](mails: Vector[A], count: Int)

object SearchResult extends SearchResultLowPriorityImplicits {
  implicit def hash[A: Hash]: Hash[SearchResult[A]] =
    Hash[(Vector[A], Int)].contramap(sr => (sr.mails, sr.count))
}

trait SearchResultLowPriorityImplicits {
  implicit def eq[A: Eq]: Eq[SearchResult[A]] =
    Eq[(Vector[A], Int)].contramap(sr => (sr.mails, sr.count))
}
