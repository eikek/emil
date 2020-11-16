package emil.javamail.conv

import java.util.Date
import jakarta.mail._
import jakarta.mail.search._

import emil.SearchQuery.Relation
import emil._

trait SearchConv {

  implicit def searchQueryConv: Conv[SearchQuery, SearchTerm] =
    Conv(makeTerm)

  private def makeTerm(q: SearchQuery): SearchTerm =
    q match {
      case SearchQuery.MessageID(id) =>
        new MessageIDTerm(id)

      case SearchQuery.ReceivedDate(date, rel) =>
        new ReceivedDateTerm(makeComparison(rel), Date.from(date))

      case SearchQuery.SentDate(date, rel) =>
        new SentDateTerm(makeComparison(rel), Date.from(date))

      case SearchQuery.Subject(text) =>
        new SubjectTerm(text)

      case SearchQuery.Flagged =>
        new FlagTerm(new Flags(Flags.Flag.FLAGGED), true)

      case SearchQuery.And(qs) =>
        new AndTerm(qs.map(makeTerm).toArray)

      case SearchQuery.RecipientTo(value) =>
        new RecipientStringTerm(Message.RecipientType.TO, value)

      case SearchQuery.RecipientCC(value) =>
        new RecipientStringTerm(Message.RecipientType.CC, value)

      case SearchQuery.RecipientBCC(value) =>
        new RecipientStringTerm(Message.RecipientType.BCC, value)

      case SearchQuery.Or(qs) =>
        new OrTerm(qs.map(makeTerm).toArray)

      case SearchQuery.Not(SearchQuery.Flagged) =>
        new FlagTerm(new Flags(Flags.Flag.FLAGGED), false)

      case SearchQuery.Not(c) =>
        new NotTerm(makeTerm(c))

      case SearchQuery.All =>
        new search.SearchTerm {
          override def `match`(msg: Message): Boolean = true
        }
    }

  private def makeComparison(rel: Relation): Int =
    rel match {
      case Relation.Equal => ComparisonTerm.EQ
      case Relation.Lt    => ComparisonTerm.LT
      case Relation.Gt    => ComparisonTerm.GT
      case Relation.Le    => ComparisonTerm.LE
      case Relation.Ge    => ComparisonTerm.GE
    }
}
