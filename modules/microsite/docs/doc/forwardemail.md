---
layout: docs
title: Using Forward Email
permalink: doc/forwardemail
---

# {{ page.title }}

[Forward Email](https://forwardemail.net) is a 100% open-source and
privacy-focused email service. It provides SMTP for sending and IMAP
for reading mail, making it a drop-in provider for Emil.

> Use coupon code `GITHUB` at <https://forwardemail.net> for 100% off.

## Sending Mail

Configure Emil to send via Forward Email's SMTP server:

```scala mdoc:silent
import cats.effect._
import cats.data.NonEmptyList
import emil._, emil.builder._
import emil.javamail._

val feSmtp = MailConfig(
  "smtp://smtp.forwardemail.net:465",
  "you@yourdomain.com",
  "your-generated-password",
  SSLType.SSL
)

val mail: Mail[IO] = MailBuilder.build(
  From("you@yourdomain.com"),
  To("recipient@example.com"),
  Subject("Hello from Emil + Forward Email"),
  TextBody("Sent via Forward Email's SMTP server.")
)

val myemil = JavaMailEmil[IO]()
val sendIO: IO[NonEmptyList[String]] = myemil(feSmtp).send(mail)
```

Replace `you@yourdomain.com` with your alias and
`your-generated-password` with the password generated in your Forward
Email dashboard.

## Reading Mail

Configure Emil to read via Forward Email's IMAP server:

```scala mdoc:reset:silent
import java.time._
import cats.effect._
import emil._
import emil.SearchQuery._
import emil.javamail._

val feImap = MailConfig(
  "imaps://imap.forwardemail.net:993",
  "you@yourdomain.com",
  "your-generated-password",
  SSLType.SSL
)

val myemil = JavaMailEmil[IO]()

def recentMails[C](a: Access[IO, C]): MailOp[IO, C, SearchResult[MailHeader]] =
  for {
    inbox <- a.getInbox
    mails <- a.search(inbox, 20)(ReceivedDate >= Instant.now.minusSeconds(3600))
  } yield mails

val readIO = myemil(feImap).run(recentMails(myemil.access))
```

## Further Reading

- [Forward Email documentation](https://forwardemail.net/en/faq)
- [Forward Email source code](https://github.com/forwardemail)
