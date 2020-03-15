# Emil - Email without a

[![Build Status](https://travis-ci.org/eikek/emil.svg?branch=master)](https://travis-ci.org/eikek/emil)
[![Scaladex](https://index.scala-lang.org/eikek/emil/latest.svg?color=blue)](https://index.scala-lang.org/eikek/emil/emil-common)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)


<a href="https://eikek.github.io/emil/">
  <img height="120" align="right" style="float:right" src="./modules/microsite/src/main/resources/microsite/img/logo.png">
</a>


`[\/]` Emil is a library for working with E-Mail in Scala. The api
builds on [Cats](https://github.com/typelevel/cats) and
[FS2](https://github.com/functional-streams-for-scala/fs2). It comes
with a backend implementation that is based on the well known [Java
Mail](https://github.com/eclipse-ee4j/mail) library. As such it is
just another wrapper library, but also a bit different:

- Simplified: a mail is a flat structure, consiting of headers, body
  and a list of attachments.
- Sending via SMTP or reading mails via IMAP is supported using same
  abstractions.

## Examples

Send a mail (returning its `messageID`):

```scala mdoc:silent
import cats.effect._
import cats.data.NonEmptyList
import emil._, emil.builder._
import emil.javamail._

implicit val CS = IO.contextShift(scala.concurrent.ExecutionContext.global)
val blocker = Blocker.liftExecutionContext(scala.concurrent.ExecutionContext.global)

val mail: Mail[IO] = MailBuilder.build(
  From("me@test.com"),
  To("test@test.com"),
  Subject("Hello!"),
  CustomHeader(Header("User-Agent", "my-email-client")),
  TextBody("Hello!\n\nThis is a mail."),
  HtmlBody("<h1>Hello!</h1>\n<p>This <b>is</b> a mail.</p>"),
  AttachUrl[IO](getClass.getResource("/files/Test.pdf"), blocker).
    withFilename("test.pdf").
    withMimeType(MimeType.pdf)
)

val myemil = JavaMailEmil[IO](blocker)
val smtpConf = MailConfig("smtp://devmail:25", "dev", "dev", SSLType.NoEncryption)

val sendIO: IO[NonEmptyList[String]] = myemil(smtpConf).send(mail)
```


Searching for mails:

```scala mdoc:reset:silent
import java.time._
import cats.effect._
import emil._
import emil.SearchQuery._
import emil.javamail._

implicit val CS = IO.contextShift(scala.concurrent.ExecutionContext.global)
val blocker = Blocker.liftExecutionContext(scala.concurrent.ExecutionContext.global)

val myemil = JavaMailEmil[IO](blocker)
val imapConf = MailConfig("imap://devmail:143", "dev", "dev", SSLType.NoEncryption)

def searchInbox[C <: Connection](a: Access[IO, C], q: SearchQuery): MailOp[IO, C, SearchResult[MailHeader]] =
  for {
    inbox  <- a.getInbox
    mails  <- a.search(inbox, 20)(q)
  } yield mails


val q = (ReceivedDate >= Instant.now.minusSeconds(60)) && (Subject =*= "test")
val searchIO: IO[SearchResult[MailHeader]] = myemil(imapConf).run(searchInbox(myemil.access, q))
```
## Documentation

More information can be found [here](https://eikek.github.io/emil/).
