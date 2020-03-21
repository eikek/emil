package emil.doobie

import java.time.Instant
import minitest._
import cats.effect._
import fs2.Stream
import _root_.doobie._
import _root_.doobie.implicits._
import emil._
import emil.builder._
import emil.doobie.EmilDoobieMeta._
import scala.concurrent.ExecutionContext

object EmilDoobieTest extends SimpleTestSuite {
  implicit val CS: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:testing;DB_CLOSE_DELAY=-1",
    "sa",
    ""
  )

  def insertRecord(r: Record) = {
    val createTable = sql"""
       create table mailaddress(
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        sender VARCHAR(255) NOT NULL,
        recipients VARCHAR(255) NOT NULL,
        ssl VARCHAR(255) NOT NULL
      )
      """

    val insertRecord = sql"""
      insert into mailaddress(sender, recipients, ssl)
      values(${r.from}, ${r.recipients}, ${r.ssl})
      """

    for {
      _ <- createTable.update.run
      id <- insertRecord.update.withUniqueGeneratedKeys[Long]("id")
    } yield id
  }

  def loadRecord(id: Long): ConnectionIO[Record] =
    sql"SELECT sender, recipients, ssl FROM mailaddress WHERE id = $id".query[Record].unique


  def insertMail(mail: Mail[IO]) = {
    val createTable = sql"""
       create table email(
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        mail TEXT NOT NULL
      )
      """

    val insert = sql"""
      insert into email(mail)
      values(${mail})
      """

    for {
      _ <- createTable.update.run
      id <- insert.update.withUniqueGeneratedKeys[Long]("id")
    } yield id
  }

  def loadMail(id: Long): ConnectionIO[Mail[IO]] =
    sql"SELECT mail FROM email WHERE id = $id".query[Mail[IO]].unique

  test("insert mail addresses") {
    val record = Record(
      MailAddress.unsafe(Some("Mr. Me"), "me@example.com"),
      List(
        MailAddress.unsafe(Some("Mr. Mine"), "mine_2@example.com"),
        MailAddress.unsafe(Some("Mr. Me"), "me@example.com"),
        MailAddress.unsafe(Some("Mr. You"), "you@example.com")
      ),
      SSLType.StartTLS
    )

    val op = for {
      id <- insertRecord(record)
      load <- loadRecord(id)
    } yield load

    val loaded = op.transact(xa).unsafeRunSync()
    assertEquals(loaded, record)
  }

  test("insert complete mail") {
    val date = Instant.now
    val mail: Mail[IO] = MailBuilder.build(
      From("me@test.com"),
      To("test@test.com"),
      Subject("Hello!"),
      MessageID("my-msg-id"),
      Date(date),
      TextBody("Hello!\n\nThis is a mail."),
      HtmlBody("<h1>Hello!</h1>\n<p>This <b>is</b> a mail.</p>"),
      AttachStream[IO](Stream.emit("hello world!").through(fs2.text.utf8Encode))
        .withFilename("test.txt")
        .withMimeType(MimeType.textPlain)
    )

    val op = for {
      id <- insertMail(mail)
      load <- loadMail(id)
    } yield load

    val loaded = op.transact(xa).unsafeRunSync()
    assertEquals(mail.header, loaded.header.copy(id = ""))
  }
}
