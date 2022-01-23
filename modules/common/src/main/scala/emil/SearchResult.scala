package emil

import cats.syntax.all._
import cats.{Eq, Hash, Monoid}

final case class SearchResult[A](mails: Vector[A], count: Int) {
  def ++(other: SearchResult[A]): SearchResult[A] =
    SearchResult(mails ++ other.mails, count + other.count)
}

object SearchResult extends SearchResultLowPriorityImplicits {
  def apply[A](mails: Vector[A]): SearchResult[A] = SearchResult(mails, mails.size)

  implicit def hash[A: Hash]: Hash[SearchResult[A]] =
    Hash[(Vector[A], Int)].contramap(sr => (sr.mails, sr.count))

  implicit def searchResultMonoid[A]: Monoid[SearchResult[A]] =
    new Monoid[SearchResult[A]] {
      def empty: SearchResult[A] = SearchResult(Vector.empty)
      def combine(x: SearchResult[A], y: SearchResult[A]): SearchResult[A] = x ++ y
    }
}

trait SearchResultLowPriorityImplicits {
  implicit def eq[A: Eq]: Eq[SearchResult[A]] =
    Eq[(Vector[A], Int)].contramap(sr => (sr.mails, sr.count))
}
