---
layout: home
position: 2
section: home
title: Examples
permalink: examples
---

# Examples

First, some imports and setup:

```scala mdoc
import cats.effect._
import emil._, emil.builder._

/* javamail backend */
import emil.javamail._
implicit val CS = IO.contextShift(scala.concurrent.ExecutionContext.global)
val blocker = Blocker.liftExecutionContext(scala.concurrent.ExecutionContext.global)
```


## Creating an e-mail

Create a simple mail:

```scala mdoc
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
```


## Sending Mails

In order to do something with it, a connection to a server is
necessary and a concrete emil:

```scala mdoc
val myemil = JavaMailEmil[IO](blocker)
val smtpConf = MailConfig("smtp://devmail:25", "dev", "dev", SSLType.NoEncryption)
```

Finally, create a program that sends the mail:

```scala mdoc
val sendIO = myemil(smtpConf).send(mail)
```

## Accessing Mails

The JavaMail backend implements IMAP access to mailboxes. First, a
connection to an imap server is necessary:

```scala mdoc
val imapConf = MailConfig("imap://devmail:143", "dev", "dev", SSLType.NoEncryption)
```

Then run an operation from the `email.access` interface:

```scala mdoc
val readIO = myemil(imapConf).run(myemil.access.getInbox)
```
