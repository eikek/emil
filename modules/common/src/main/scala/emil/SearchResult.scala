package emil

final case class SearchResult[A](mails: Vector[A], count: Int) {}
