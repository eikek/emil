package emil

import java.time.Instant

import cats.effect._
import emil.builder._
import emil.test.GreenmailTestSuite

abstract class AbstractAccessTest[A, C <: Connection] extends GreenmailTestSuite[A] {
  def emil: Emil[IO, C]

  val user1 = MailAddress.unsafe(None, "joe@test.com")
  val user2 = MailAddress.unsafe(None, "joan@test.com")

  def users: List[MailAddress] = List(user1, user2)

  def user1Imap = emil(imapConf(user1))

  override def tearDown(env: A): Unit =
    server.removeAllMails()

  test("get inbox") { _ =>
    val inbox = emil(imapConf(user1)).run(emil.access.getInbox).unsafeRunSync()
    assertEquals(inbox, MailFolder("INBOX", "INBOX"))
  }

  test("create folder") { _ =>
    val folder = user1Imap.run(emil.access.createFolder(None, "test1")).unsafeRunSync()
    val subfolder = user1Imap.run(emil.access.createFolder(Some(folder), "test2")).unsafeRunSync()

    assertEquals(folder, MailFolder("test1", "test1"))
    assertEquals(subfolder, MailFolder("test1.test2", "test2"))

    val inbox = user1Imap.run(emil.access.getInbox).unsafeRunSync()
    val inboxSub = user1Imap.run(emil.access.createFolder(Some(inbox), "archived")).unsafeRunSync()
    assertEquals(inboxSub, MailFolder("INBOX.archived", "archived"))
  }

  test("find folder") { _ =>
    val makeFolder = for {
      inbox <- emil.access.getInbox
      folder <- emil.access.createFolder(Some(inbox), "myfolder")
    } yield folder

    def find(name: String) =
      for {
        inbox <- emil.access.getInbox
        folder <- emil.access.findFolder(Some(inbox), name)
      } yield folder

    val inboxSub = user1Imap.run(makeFolder).unsafeRunSync()
    val found = user1Imap.run(find("myfolder")).unsafeRunSync()
    assertEquals(found, Some(inboxSub))
  }

  test("get message count") { _ =>
    val msgCount = for {
      inbox <- emil.access.getInbox
      count <- emil.access.getMessageCount(inbox)
    } yield count

    val n = user1Imap.run(msgCount).unsafeRunSync()
    assertEquals(n, 0)

    emil(smtpConf(user2))
      .send(
        (1 to 5).map(
          n =>
            MailBuilder.build[IO](
              From(user2),
              To(user1),
              Subject(s"Hello $n!"),
              TextBody(s"This is text $n.")
            )
        ): _*
      )
      .unsafeRunSync()
    server.waitForReceive(5)

    val n1 = user1Imap.run(msgCount).unsafeRunSync()
    assertEquals(n1, 5)
  }

  test("search message") { _ =>
    emil(smtpConf(user2))
      .send(
        (1 to 5).map(
          n =>
            MailBuilder.build[IO](
              From(user2),
              To(user1),
              Subject(s"Hello $n!"),
              TextBody(s"This is text $n.")
            )
        ): _*
      )
      .unsafeRunSync()
    server.waitForReceive(5)

    def assertSearch(q: SearchQuery, count: Int) =
      user1Imap
        .run(for {
          inbox <- emil.access.getInbox
          result <- emil.access.search(inbox, Int.MaxValue)(q)
          _ = assertEquals(result.count, count)
        } yield ())
        .unsafeRunSync()

    import SearchQuery._

    assertSearch(ReceivedDate > Instant.now.minusSeconds(2), 5)
    assertSearch((ReceivedDate > Instant.now.minusSeconds(3)) && Flagged, 0)
    assertSearch(Subject =*= "hello", 5)
    assertSearch(Subject =*= "hello 2", 1)
    assertSearch((Subject =*= "hello 1") || (Subject =*= "hello 3"), 2)
  }

  test("load mail") { _ =>
    emil(smtpConf(user2))
      .send(
        MailBuilder.build(
          From(user2),
          To(user1),
          Subject(s"Hello and Attach!"),
          TextBody(s"This is text."),
          AttachUrl[IO](getClass.getResource("/files/Test.pdf"), blocker)
            .withFilename("Test.pdf")
            .withMimeType(MimeType.pdf)
        )
      )
      .unsafeRunSync()
    server.waitForReceive(1)

    val mail = user1Imap
      .run(for {
        inbox <- emil.access.getInbox
        mail <- emil.access.searchAndLoad(inbox, 1)(SearchQuery.All)
      } yield mail)
      .unsafeRunSync()
      .mails
      .head

    assertEquals(mail.header.subject, "Hello and Attach!")
    assertEquals(mail.attachments.size, 1)
    assertEquals(mail.attachments.all.head.length.unsafeRunSync(), 16571L)
    assertEquals(mail.attachments.all.head.filename, Some("Test.pdf"))
    assertEquals(mail.attachments.all.head.mimeType.baseType, MimeType.pdf)
    val checksum = mail.attachments.all.head.content
      .through(fs2.hash.sha256)
      .chunks
      .map(_.toByteVector.toHex)
      .compile
      .lastOrError
      .unsafeRunSync()
    assertEquals(checksum, "10c223f016887635e25b24fe40cc00a9b06fdd8656428288916a82010ebcc61a")
  }

  test("move mail") { _ =>
    emil(smtpConf(user2))
      .send(
        MailBuilder.build(
          From(user2),
          To(user1),
          Subject(s"Hello and Attach!"),
          TextBody(s"This is text.")
        )
      )
      .unsafeRunSync()
    server.waitForReceive(1)

    val findAndMove = for {
      inbox <- emil.access.getInbox
      res <- emil.access.search(inbox, 1)(SearchQuery.All)
      mail = res.mails.head
      target <- emil.access.getOrCreateFolder(None, "TestFolder")
      _ <- emil.access.moveMail(mail, target)
    } yield mail.messageId

    val orgId = user1Imap.run(findAndMove).unsafeRunSync()

    val check = for {
      fopt <- emil.access.findFolder(None, "TestFolder")
      folder = fopt.getOrElse(sys.error("folder not found"))
      res <- emil.access.search(folder, 1)(SearchQuery.All)
      mail = res.mails.head
    } yield mail.messageId

    val movedId = user1Imap.run(check).unsafeRunSync()
    assertEquals(movedId, orgId)
    assert(movedId.isDefined)
  }

  test("delete mail") { _ =>
    emil(smtpConf(user2))
      .send(
        MailBuilder.build(
          From(user2),
          To(user1),
          Subject(s"Hello and Attach!"),
          TextBody(s"This is text.")
        )
      )
      .unsafeRunSync()
    server.waitForReceive(1)

    val findAndDelete = for {
      inbox <- emil.access.getInbox
      res <- emil.access.search(inbox, 1)(SearchQuery.All)
      mail = res.mails.head
      _ <- emil.access.deleteMail(mail)
    } yield ()

    user1Imap.run(findAndDelete).unsafeRunSync()

    val find = for {
      inbox <- emil.access.getInbox
      n <- emil.access.getMessageCount(inbox)
      _ = assertEquals(n, 0)
    } yield ()
    user1Imap.run(find).unsafeRunSync()
  }

  test("search delete") { _ =>
    emil(smtpConf(user2))
      .send(
        (1 to 5).map(
          n =>
            MailBuilder.build[IO](
              From(user2),
              To(user1),
              Subject(s"Hello $n!"),
              TextBody(s"This is text $n.")
            )
        ): _*
      )
      .unsafeRunSync()
    server.waitForReceive(5)

    import SearchQuery._

    val delete2 = for {
      inbox <- emil.access.getInbox
      n <- emil.access.searchDelete(inbox, 10)((Subject =*= "hello 1") || (Subject =*= "hello 2"))
    } yield n.count

    val n = user1Imap.run(delete2).unsafeRunSync()
    assertEquals(n, 2)

    val findRest = for {
      inbox <- emil.access.getInbox
      res <- emil.access.search(inbox, 10)(SearchQuery.All)
    } yield res.count
    val rest = user1Imap.run(findRest).unsafeRunSync()
    assertEquals(rest, 3)
  }

  test("copy mail") { _ =>
    emil(smtpConf(user2))
      .send(
        MailBuilder.build(
          From(user2),
          To(user1),
          Subject(s"Hello and Attach!"),
          TextBody(s"This is text.")
        )
      )
      .unsafeRunSync()
    server.waitForReceive(1)

    val findAndCopy = for {
      inbox <- emil.access.getInbox
      res <- emil.access.search(inbox, 1)(SearchQuery.All)
      mail = res.mails.head
      target <- emil.access.getOrCreateFolder(None, "TestFolder")
      _ <- emil.access.copyMail(mail, target)
    } yield mail.messageId

    val orgId = user1Imap.run(findAndCopy).unsafeRunSync()

    val check1 = for {
      fopt <- emil.access.findFolder(None, "TestFolder")
      folder = fopt.getOrElse(sys.error("folder not found"))
      res <- emil.access.search(folder, 1)(SearchQuery.All)
      mail = res.mails.head
    } yield mail.messageId

    val check2 = for {
      inbox <- emil.access.getInbox
      res <- emil.access.search(inbox, 1)(SearchQuery.All)
      mail = res.mails.head
    } yield mail.messageId

    val movedId = user1Imap.run(check1).unsafeRunSync()
    val existid = user1Imap.run(check2).unsafeRunSync()
    assertEquals(movedId, orgId)
    assertEquals(movedId, existid)
    assert(movedId.isDefined)
  }

  test("put mail 1") { _ =>
    val newMail: Mail[IO] = MailBuilder.build(
      From(user2),
      To(user1),
      Subject(s"Hello and Attach!"),
      TextBody(s"This is text.")
    )

    val put = for {
      inbox <- emil.access.getInbox
      _ <- emil.access.putMail(newMail, inbox)
    } yield ()
    user1Imap.run(put).unsafeRunSync()

    val check = for {
      inbox <- emil.access.getInbox
      res <- emil.access.search(inbox, 1)(SearchQuery.All)
      mail = res.mails.head
    } yield mail
    val inMail = user1Imap.run(check).unsafeRunSync()

    assertEquals(newMail.header.subject, inMail.subject)
    assert(inMail.messageId.isDefined)
  }

  test("put mail 2") { _ =>
    val newMail: Mail[IO] = MailBuilder.build(
      From(user2),
      To(user1),
      MessageID("<my-id-2>"),
      Subject(s"Hello and Attach!"),
      TextBody(s"This is text.")
    )

    val put = for {
      inbox <- emil.access.getInbox
      _ <- emil.access.putMail(newMail, inbox)
    } yield ()
    user1Imap.run(put).unsafeRunSync()

    val check = for {
      inbox <- emil.access.getInbox
      res <- emil.access.search(inbox, 1)(SearchQuery.All)
      mail = res.mails.head
    } yield mail
    val inMail = user1Imap.run(check).unsafeRunSync()

    assertEquals(newMail.header.subject, inMail.subject)
    assertEquals(inMail.messageId, Some("<my-id-2>"))
  }
}
