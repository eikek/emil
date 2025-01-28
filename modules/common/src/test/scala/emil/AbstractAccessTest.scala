package emil

import java.time.Instant

import _root_.emil.builder._
import _root_.emil.test.GreenmailTestSuite
import cats.data.NonEmptyList
import cats.effect._
import cats.effect.unsafe.implicits.global
import fs2.hashing.{HashAlgorithm, Hashing}

abstract class AbstractAccessTest(val emil: Emil[IO]) extends GreenmailTestSuite {

  val user1 = MailAddress.unsafe(None, "joe@test.com")
  val user2 = MailAddress.unsafe(None, "joan@test.com")

  def users: List[MailAddress] = List(user1, user2)

  def user1Imap: Emil.Run[IO, emil.C] = emil(imapConf(user1))

  def makeMail(n: Int): Mail[IO] =
    MailBuilder.build[IO](
      From(user2),
      To(user1),
      Subject(s"Hello $n!"),
      TextBody(s"This is text $n.")
    )

  override def afterEach(ctx: AfterEach): Unit = {
    if (server != null) {
      server.removeAllMails()
    }
    super.afterAll()
  }

  test("get inbox") {
    val op: MailOp[IO, emil.C, MailFolder] = emil.access.getInbox
    val inbox = user1Imap.run(op).unsafeRunSync()
    assertEquals(inbox, MailFolder("INBOX", NonEmptyList.of("INBOX")))
  }

  test("create folder") {
    val folder = user1Imap.run(emil.access.createFolder(None, "test1")).unsafeRunSync()
    val subfolder =
      user1Imap.run(emil.access.createFolder(Some(folder), "test2")).unsafeRunSync()

    assertEquals(folder, MailFolder("test1", NonEmptyList.of("test1")))
    assertEquals(subfolder, MailFolder("test1.test2", NonEmptyList.of("test1", "test2")))

    val inbox = user1Imap.run(emil.access.getInbox).unsafeRunSync()
    val inboxSub =
      user1Imap.run(emil.access.createFolder(Some(inbox), "archived")).unsafeRunSync()
    assertEquals(
      inboxSub,
      MailFolder("INBOX.archived", NonEmptyList.of("INBOX", "archived"))
    )
  }

  test("find folder") {
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

  test("get message count") {
    val msgCount = for {
      inbox <- emil.access.getInbox
      count <- emil.access.getMessageCount(inbox)
    } yield count

    val n = user1Imap.run(msgCount).unsafeRunSync()
    assertEquals(n, 0)

    emil(smtpConf(user2))
      .send_(NonEmptyList(makeMail(1), (2 to 5).map(makeMail).toList))
      .unsafeRunSync()
    server.waitForReceive(5)

    val n1 = user1Imap.run(msgCount).unsafeRunSync()
    assertEquals(n1, 5)
  }

  test("search message") {
    emil(smtpConf(user2))
      .send_(NonEmptyList(makeMail(1), (2 to 5).map(makeMail).toList))
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

  test("load mail") {
    emil(smtpConf(user2))
      .send(
        MailBuilder.build(
          From(user2),
          To(user1),
          Subject(s"Hello and Attach!"),
          TextBody(s"This is text."),
          AttachUrl[IO](getClass.getResource("/files/Test.pdf"))
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
      .through(Hashing.forSync[IO].hash(HashAlgorithm.SHA256))
      .map(_.bytes)
      .map(_.toByteVector.toHex)
      .compile
      .lastOrError
      .unsafeRunSync()
    assertEquals(
      checksum,
      "10c223f016887635e25b24fe40cc00a9b06fdd8656428288916a82010ebcc61a"
    )
  }

  test("move mail") {
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

  test("delete mail") {
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

  test("search delete") {
    emil(smtpConf(user2))
      .send_(NonEmptyList(makeMail(1), (2 to 5).map(makeMail).toList))
      .unsafeRunSync()
    server.waitForReceive(5)

    import SearchQuery._

    val delete2 = for {
      inbox <- emil.access.getInbox
      n <- emil.access.searchDelete(inbox, 10)(
        (Subject =*= "hello 1") || (Subject =*= "hello 2")
      )
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

  test("copy mail") {
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
    val existId = user1Imap.run(check2).unsafeRunSync()
    assertEquals(movedId, orgId)
    assertEquals(movedId, existId)
    assert(movedId.isDefined)
  }

  test("put mail 1") {
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

  test("put mail 2") {
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

  test("list inbox folders") {
    val makeFolders = for {
      inbox <- emil.access.getInbox
      folder1 <- emil.access.createFolder(Some(inbox), "myfolder1")
      folder2 <- emil.access.createFolder(Some(inbox), "myfolder2")
    } yield Vector(folder1, folder2)

    def listInbox() =
      for {
        inbox <- emil.access.getInbox
        folders <- emil.access.listFolders(Some(inbox))
      } yield folders

    val inboxFolders = user1Imap.run(makeFolders).unsafeRunSync()
    val listed = user1Imap.run(listInbox()).unsafeRunSync()
    assertEquals(listed, inboxFolders)

    assertEquals(listed(0).path, NonEmptyList.of("INBOX", "myfolder1"))
    assertEquals(listed(1).path, NonEmptyList.of("INBOX", "myfolder2"))
  }

  test("list root folders") {
    val makeFolders = for {
      folder1 <- emil.access.createFolder(None, "myfolder1")
      folder2 <- emil.access.createFolder(None, "myfolder2")
    } yield Vector(folder1, folder2)

    val inbox = user1Imap.run(emil.access.getInbox).unsafeRunSync()

    val folders = user1Imap.run(makeFolders).unsafeRunSync()
    val foldersWithInbox = Vector(inbox) ++ folders
    val listed = user1Imap.run(emil.access.listFolders(None)).unsafeRunSync()
    assertEquals(listed, foldersWithInbox)

    assertEquals(listed(0).path, NonEmptyList.one("INBOX"))
    assertEquals(listed(1).path, NonEmptyList.one("myfolder1"))
    assertEquals(listed(2).path, NonEmptyList.one("myfolder2"))
  }

  test("list root folder recursively") {
    val makeFolders = for {
      folder1 <- emil.access.createFolder(None, "myfolder1")
      folder11 <- emil.access.createFolder(Some(folder1), "myfolder11")
      folder12 <- emil.access.createFolder(Some(folder1), "myfolder12")
      folder13 <- emil.access.createFolder(Some(folder1), "myfolder13")
      folder2 <- emil.access.createFolder(None, "myfolder2")
      folder21 <- emil.access.createFolder(Some(folder2), "myfolder21")
      folder211 <- emil.access.createFolder(Some(folder21), "myfolder211")
    } yield Vector(folder1, folder11, folder12, folder13, folder2, folder21, folder211)

    val inbox = user1Imap.run(emil.access.getInbox).unsafeRunSync()

    val folders = user1Imap.run(makeFolders).unsafeRunSync()
    val foldersWithInbox = Vector(inbox) ++ folders
    val listed = user1Imap.run(emil.access.listFoldersRecursive(None)).unsafeRunSync()

    assert(listed.size == foldersWithInbox.size)

    assert(listed.exists(x => x.path == NonEmptyList.one("INBOX")))
    assert(listed.exists(x => x.path == NonEmptyList.one("myfolder1")))
    assert(listed.exists(x => x.path == NonEmptyList.one("myfolder2")))
    assert(listed.exists(x => x.path == NonEmptyList.of("myfolder1", "myfolder11")))
    assert(listed.exists(x => x.path == NonEmptyList.of("myfolder1", "myfolder12")))
    assert(listed.exists(x => x.path == NonEmptyList.of("myfolder1", "myfolder13")))
    assert(listed.exists(x => x.path == NonEmptyList.of("myfolder2", "myfolder21")))
    assert(
      listed.exists(x =>
        x.path == NonEmptyList.of("myfolder2", "myfolder21", "myfolder211")
      )
    )
  }

  test("list folder recursively") {
    val makeFolders = for {
      folder1 <- emil.access.createFolder(None, "myfolder1")
      folder11 <- emil.access.createFolder(Some(folder1), "myfolder11")
      folder12 <- emil.access.createFolder(Some(folder1), "myfolder12")
      folder13 <- emil.access.createFolder(Some(folder1), "myfolder13")
      folder131 <- emil.access.createFolder(Some(folder13), "myfolder131")
      folder2 <- emil.access.createFolder(None, "myfolder2")
      folder21 <- emil.access.createFolder(Some(folder2), "myfolder21")
      folder211 <- emil.access.createFolder(Some(folder21), "myfolder211")
    } yield Vector(
      folder1,
      folder11,
      folder12,
      folder13,
      folder131,
      folder2,
      folder21,
      folder211
    )

    def listFolderRecursively(name: String) =
      for {
        folder <- emil.access.findFolder(None, name)
        folders <- emil.access.listFoldersRecursive(folder)
      } yield folders

    user1Imap.run(makeFolders).unsafeRunSync()
    val listed = user1Imap.run(listFolderRecursively("myfolder1")).unsafeRunSync()

    assert(listed.size == 4)

    assert(!listed.exists(x => x.path == NonEmptyList.one("myfolder1")))

    assert(listed.exists(x => x.path == NonEmptyList.of("myfolder1", "myfolder11")))
    assert(listed.exists(x => x.path == NonEmptyList.of("myfolder1", "myfolder12")))
    assert(listed.exists(x => x.path == NonEmptyList.of("myfolder1", "myfolder13")))
    assert(
      listed.exists(x =>
        x.path == NonEmptyList.of("myfolder1", "myfolder13", "myfolder131")
      )
    )
  }
}
