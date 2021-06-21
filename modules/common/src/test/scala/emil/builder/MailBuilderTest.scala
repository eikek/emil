package emil.builder

import cats.effect._
import cats.implicits._
import emil._
import munit._

class MailBuilderTest extends FunSuite {

  test("combine trans") {
    val t = Trans.combineAll[IO](Seq(Subject("test"), From("test@test.com")))
    val m = t(Mail.empty[IO])
    assertEquals(m.header.subject, "test")
    assertEquals(m.header.from, MailAddress.unsafe(None, "test@test.com").some)
  }

  test("optional custom header: none") {
    val m = ListId[IO](None)(Mail.empty)
    assertEquals(m.additionalHeaders.find("List-Id"), None)
    assertEquals(m, Mail.empty[IO])
  }

  test("optional custom header: some") {
    val m = ListId[IO](Some("<the-list-id>"))(Mail.empty)
    assertEquals(
      m.additionalHeaders.find("list-id").map(_.value.head),
      Some("<the-list-id>")
    )
    assertEquals(
      m.additionalHeaders.find("List-Id").map(_.value.head),
      Some("<the-list-id>")
    )
  }

  test("optional custom header: some empty") {
    val m = MessageID[IO](Some(""))(Mail.empty)
    assertEquals(m, Mail.empty[IO])
  }

  test("set header if non-empty") {
    val m = MessageID[IO]("abcde")(Mail.empty)
    assertEquals(m.header.messageId, Some("abcde"))
  }

  test("don't set header if empty") {
    val m = UserAgent[IO]("")(Mail.empty)
    assertEquals(m, Mail.empty[IO])
  }

  test("set header if empty") {
    val m = CustomHeader[IO]("x-my-header", "").allowEmpty(Mail.empty)
    assertEquals(m.additionalHeaders.find("x-my-header").map(_.value.head), Some(""))
  }
}
