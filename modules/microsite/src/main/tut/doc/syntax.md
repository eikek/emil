---
layout: docs
title: Syntax Utilities
---

# {{ page.title }}

The `emil-common` module doesn't provide utilities for parsing mails
or mail addresses from text. This is provided by the `emil-javamail`
module, because the implementation from JavaMail is used.

There is a `syntax` object that has some common utilities when
reading/writing e-mail.


## Parsing MimeType from String

```tut:book
import emil.javamail.syntax._
import emil._

MimeType.parse("text/html; charset=utf-8")
```

## Reading/Writing E-Mail addresses

Parse a string into an e-mail address:

```tut:book
MailAddress.parse("John Doe <jdoe@gmail.com>")
MailAddress.parse("John Doe doe@com")
```

Write the mail address as unicode or ascii-only string:


```tut:book
val ma = MailAddress.parse("Örtlich <über.uns@test.com>").toOption.get
val ascii = ma.asAsciiString
MailAddress.parse(ascii)
val unicode = ma.asUnicodeString
```


## E-Mail from/to String

E-Mail as string:

```tut:book
import cats.effect._
import emil._, emil.builder._

val mail = MailBuilder.build[IO](
  From("test@test.com"),
  To("test@test.com"),
  Subject("Hello"),
  HtmlBody("<p>This is html</p>")
)

import emil.javamail.syntax._

val mailStr = mail.serialize.unsafeRunSync
```

Deserialize:

```tut:book
val mail2 = Mail.deserialize[IO](mailStr).unsafeRunSync

```
