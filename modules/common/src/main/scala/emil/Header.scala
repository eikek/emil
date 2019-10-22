package emil

import cats.data.NonEmptyList
import cats.implicits._

final case class Header(name: String, value: NonEmptyList[String]) {

  def append(v: String): Header =
    Header(name, (value :+ v).distinct)

  def append(vs: List[String]): Header =
    Header(name, (value ++ vs).distinct)

  def noneOf(v: String, vm: String*): Boolean =
    (v +: vm).forall(s => !name.equalsIgnoreCase(s))
}

object Header {
  def apply(name: String, value: String, more: String*): Header =
    Header(name, NonEmptyList.of(value, more: _*))

  // define some popular ones here
  def inReplyTo(value: String, more: String*): Header =
    Header("In-Reply-To", NonEmptyList.of(value, more: _*))
}
