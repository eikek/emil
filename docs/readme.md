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

- Extensible DSL for creating mails in code.
- Conveniently send mails via SMTP.
- Search mail boxes via IMAP.
- The data structures model a simplified E-Mail structure. Instead of
  adhering to the recursive structure of a mime message, a mail here
  is flat, consisting of a header, a body (text, html or both) and a
  list of attachments.
- The data structures and api are in a separate module, that doesn't
  depend on a concrete implementation library, like Java Mail. An
  implementation based on
  [fs2-mail](https://github.com/Spinoco/fs2-mail) or even
  [EWS](https://github.com/OfficeDev/ews-java-api) can be created
  without affecting the user code of this library.

Write your e-mail related code once and then decide how to execute.

## Usage

With [sbt](https://scala-sbt.org), add the dependencies:

```
"com.github.eikek" %% "emil-common" % "@VERSION@"  // the core library
"com.github.eikek" %% "emil-javamail" % "@VERSION@" // implementation module
// … optionally, other modules analog
```

Emil is provided for Scala 2.12, 2.13 and 3.


## Examples

Send a mail (returning its `messageID`):

```scala mdoc:silent
import cats.effect._
import cats.data.NonEmptyList
import emil._, emil.builder._
import emil.javamail._

val mail: Mail[IO] = MailBuilder.build(
  From("me@test.com"),
  To("test@test.com"),
  Subject("Hello!"),
  CustomHeader(Header("User-Agent", "my-email-client")),
  TextBody("Hello!\n\nThis is a mail."),
  HtmlBody("<h1>Hello!</h1>\n<p>This <b>is</b> a mail.</p>"),
  AttachUrl[IO](getClass.getResource("/files/Test.pdf")).
    withFilename("test.pdf").
    withMimeType(MimeType.pdf)
)

val myemil = JavaMailEmil[IO]()
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

val myemil = JavaMailEmil[IO]()
val imapConf = MailConfig("imap://devmail:143", "dev", "dev", SSLType.NoEncryption)

def searchInbox[C](a: Access[IO, C], q: SearchQuery): MailOp[IO, C, SearchResult[MailHeader]] =
  for {
    inbox  <- a.getInbox
    mails  <- a.search(inbox, 20)(q)
  } yield mails


val q = (ReceivedDate >= Instant.now.minusSeconds(60)) && (Subject =*= "test")
val searchIO: IO[SearchResult[MailHeader]] = myemil(imapConf).run(searchInbox(myemil.access, q))
```
## Documentation

More information can be found [here](https://eikek.github.io/emil/).
