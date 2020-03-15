package emil

import java.time.Instant
import cats.implicits._
import cats.kernel.Semigroup
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime

/** Received header as described in RCF2822.
  *
  * https://tools.ietf.org/html/rfc2822
  */
case class Received(data: Vector[(String, String)], date: Instant) {

  def findFirst(name: String): Option[String] =
    data.find(_._1.equalsIgnoreCase(name)).map(_._2)
}

object Received {

  def parse(str: String): Either[String, Received] =
    Parser.received(str.trim).map(_._2)

  def parseDateTime(str: String): Either[String, Instant] = {
    val san = str.trim
      .replaceAll("""\([A-Za-z]+\)""", "")
      .replaceAll("\\.[0-9]+", "")
      .trim
    Either
      .catchNonFatal(ZonedDateTime.parse(san, DateTimeFormatter.RFC_1123_DATE_TIME))
      .map(_.toInstant)
      .leftMap(_.getMessage)
  }

  private[emil] object Parser {
    type P[A] = String => Either[String, (String, A)]
    val alphaNum = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).toSet

    implicit final class POps[A](p: P[A]) {
      def map[B](f: A => B): P[B] =
        in => p(in).map({ case (rest, a) => (rest, f(a)) })

      def emap[B](f: A => Either[String, B]): P[B] =
        in =>
          p(in).flatMap({
            case (rest, a) =>
              f(a).map(b => rest -> b)
          })

      def ~[B](n: P[B]): P[(A, B)] =
        in =>
          p(in) match {
            case Right((rest, a)) =>
              n(rest) match {
                case Right((r2, b)) => Right((r2, (a, b)))
                case Left(err)      => Left(err)
              }
            case Left(err) => Left(err)
          }

      def ~>[B](n: P[B]): P[B] =
        (p ~ n).map(_._2)

      def <~[B](n: P[B]): P[A] =
        (p ~ n).map(_._1)

      def ++(n: P[A])(implicit S: Semigroup[A]): P[A] =
        (p ~ n).map({ case (a, b) => S.combine(a, b) })

      def opt: P[Option[A]] =
        in =>
          p(in) match {
            case Right((rest, a)) => Right(rest -> a.some)
            case Left(_)          => Right((in, None))
          }

      def rep: P[Vector[A]] = {
        @annotation.tailrec
        def go(result: Vector[A], in: String): Either[String, (String, Vector[A])] =
          p(in) match {
            case Right((rest, a)) => go(result :+ a, rest)
            case Left(_)          => Right(in -> result)
          }

        str => go(Vector.empty, str)
      }

      def repsep(sep: P[_]): P[Vector[A]] =
        ((p <~ sep).rep ~ p.opt).map {
          case (list, el) => list ++ el.toSeq
        }
    }

    def rest: P[String] =
      in => Right("" -> in)

    def stringIn(chars: Set[Char]): P[String] =
      in => {
        val next = in.takeWhile(chars contains _)
        if (next.isEmpty) Left(s"Expected chars in ${chars.toList.sorted.mkString}, but got: $in")
        else Right(in.substring(next.length) -> next)
      }

    def stringNotIn(chars: Set[Char]): P[String] =
      in => {
        val next = in.takeWhile(c => !chars.contains(c))
        if (next.isEmpty)
          Left(s"Expected chars not in ${chars.toList.sorted.mkString}, but got: $in")
        else Right(in.substring(next.length) -> next)
      }

    def const(str: String): P[String] =
      in => {
        if (in.startsWith(str)) Right(in.substring(str.length) -> str)
        else Left(s"Expected $str, but got: $in")
      }

    def ws: P[String] =
      stringIn(" \t\n\r".toSet)

    def itemName: P[String] =
      stringIn(alphaNum ++ Set('-'))

    def comment: P[String] =
      const("(") ++ stringNotIn(Set(')')) ++ const(")")

    def itemValue: P[String] =
      (stringNotIn(" \t\n\r;".toSet) ~ (ws ++ comment).rep).map {
        case (v1, vec) => v1 + vec.mkString(" ")
      }

    def nameValue: P[(String, String)] =
      itemName ~ (ws ~> itemValue)

    def nameValueList: P[Vector[(String, String)]] =
      nameValue.repsep(ws)

    def date: P[Instant] =
      rest.emap(parseDateTime)

    def received: P[Received] =
      (nameValueList ~ (const(";") ~> date)).map {
        case (data, dt) => Received(data, dt)
      }
  }
}
