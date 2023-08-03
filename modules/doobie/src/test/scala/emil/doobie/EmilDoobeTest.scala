package emil.doobie

package emil.doobie

import _root_.doobie._
import _root_.doobie.implicits._
import _root_.emil._
import _root_.emil.doobie.EmilDoobieMeta._
import cats.effect._
import cats.effect.unsafe.implicits.global
import munit._

class EmilDoobieTest extends FunSuite {

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:testing;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  def insertRecord(r: Record) = {
    val createTable = sql"""
       create table mailaddress(
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        sender VARCHAR(255) NOT NULL,
        recipients VARCHAR(255) NOT NULL,
        ssl VARCHAR(255) NOT NULL,
        mime VARCHAR(255) NOT NULL
      )
      """

    val insertRecord = sql"""
      insert into mailaddress(sender, recipients, ssl, mime)
      values(${r.from}, ${r.recipients}, ${r.ssl}, ${r.mime})
      """

    for {
      _ <- createTable.update.run
      id <- insertRecord.update.withUniqueGeneratedKeys[Long]("id")
    } yield id
  }

  def loadRecord(id: Long): ConnectionIO[Record] =
    sql"SELECT sender, recipients, ssl, mime FROM mailaddress WHERE id = $id"
      .query[Record]
      .unique

  test("insert mail addresses") {
    val record = Record(
      MailAddress.unsafe(Some("Mr. Me"), "me@example.com"),
      List(
        MailAddress.unsafe(Some("Mr. Mine"), "mine_2@example.com"),
        MailAddress.unsafe(Some("Mr. Me"), "me@example.com"),
        MailAddress.unsafe(Some("Mr. You"), "you@example.com")
      ),
      SSLType.StartTLS,
      MimeType.textHtml
    )

    val op = for {
      id <- insertRecord(record)
      load <- loadRecord(id)
    } yield load

    val loaded = op.transact(xa).unsafeRunSync()
    assertEquals(loaded, record)
  }
}
