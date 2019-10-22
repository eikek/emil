package emil

import java.time.Instant

sealed trait SearchQuery {

  def &&(q: SearchQuery): SearchQuery.And =
    SearchQuery.And(this, q)

  def ||(q: SearchQuery): SearchQuery.Or =
    SearchQuery.Or(this, q)

  def unary_! : SearchQuery =
    SearchQuery.Not(this)
}

object SearchQuery {

  sealed trait Relation
  object Relation {
    case object Equal extends Relation
    case object Lt extends Relation
    case object Gt extends Relation
    case object Le extends Relation
    case object Ge extends Relation
  }

  trait ContainsCompanion[A <: SearchQuery, B] {
    def make(value: B, relation: Relation): A

    def contains(value: B): A = make(value, Relation.Equal)
    def =*=(value: B): A = contains(value)

    def notContains(value: B) = Not(contains(value))
    def !== (value: B) = notContains(value)
  }

  trait ComparisonCompanion[A <: SearchQuery, B] extends ContainsCompanion[A, B] {
    def greaterThan(value: B): A = make(value, Relation.Gt)
    def > (value: B): A = greaterThan(value)

    def lowerThan(value: B): A = make(value, Relation.Lt)
    def < (value: B): A = lowerThan(value)

    def greaterEqual(value: B): A = make(value, Relation.Ge)
    def >= (value: B): A = greaterEqual(value)

    def lowerEqual(value: B): A = make(value, Relation.Le)
    def <= (value: B): A = lowerEqual(value)
  }

  case object All extends SearchQuery

  case class MessageID(id: String) extends SearchQuery
  object MessageID extends ContainsCompanion[MessageID, String] {
    def make(value: String, rel: Relation): MessageID = MessageID(value)
  }

  case class ReceivedDate(date: Instant, rel: Relation) extends SearchQuery

  object ReceivedDate extends ComparisonCompanion[ReceivedDate, Instant] {
    def make(value: Instant, relation: Relation): ReceivedDate =
      ReceivedDate(value, relation)
  }

  case class SentDate(date: Instant, rel: Relation) extends SearchQuery
  object SentDate extends ComparisonCompanion[SentDate, Instant] {
    def make(value: Instant, rel: Relation): SentDate =
      SentDate(value, rel)
  }

  case class Subject(text: String) extends SearchQuery
  object Subject extends ContainsCompanion[Subject, String] {
    def make(value: String, rel: Relation): Subject =
      Subject(value)
  }
  case class And(qs: Seq[SearchQuery]) extends SearchQuery {
    override def &&(q: SearchQuery): And =
      And(q +: qs)
  }

  case object Flagged extends SearchQuery

  case class RecipientTo(value: String) extends SearchQuery
  object RecipientTo extends ContainsCompanion[RecipientTo, String] {
    def make(value: String, rel: Relation) = RecipientTo(value)
  }

  case class RecipientCC(value: String) extends SearchQuery
  object RecipientCC extends ContainsCompanion[RecipientCC, String] {
    def make(value: String, rel: Relation) = RecipientCC(value)
  }

  case class RecipientBCC(value: String) extends SearchQuery
  object RecipientBCC extends ContainsCompanion[RecipientBCC, String] {
    def make(value: String, rel: Relation) = RecipientBCC(value)
  }

  object AnyRecipient extends ContainsCompanion[SearchQuery, String] {
    def make(value: String, rel: Relation) = apply(value)

    def apply(value: String): SearchQuery =
      Or(RecipientTo(value), RecipientCC(value), RecipientBCC(value))
  }

  object And {
    def apply(q0: SearchQuery, qs: SearchQuery*): And =
      And(Seq(q0) ++ qs)
  }

  case class Or(qs: Seq[SearchQuery]) extends SearchQuery {
    override def ||(q: SearchQuery): Or =
      Or(qs :+ q)
  }

  object Or {
    def apply(q0: SearchQuery, qs: SearchQuery*): Or =
      Or(Seq(q0) ++ qs)
  }

  case class Not(c: SearchQuery) extends SearchQuery {
    override def unary_! : SearchQuery = c
  }
}
