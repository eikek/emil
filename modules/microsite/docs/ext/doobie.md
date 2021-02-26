---
layout: docs
title: Doobie Integration
permalink: ext/doobie
---

# {{ page.title }}

The module `emil-doobie` provides `Meta` instances for
[Doobie](https://github.com/tpolecat/doobie).

## Usage

With sbt:

```
libraryDependencies += "com.github.eikek" %% "emil-doobie" % "@VERSION@"
```


## Description

You can use emil data types `MailAddress`, `SSLType` and `Mail` in
your record classes.

```scala mdoc:silent
import emil._

case class Record(from: MailAddress, recipients: List[MailAddress], ssl: SSLType, mime: MimeType)
```

In order to use these types in SQL, you need to import instances
defined in `emil.doobie.EmilDoobieMeta`.

```scala mdoc
import emil.doobie.EmilDoobieMeta._
import _root_.doobie.implicits._

val r = Record(
  MailAddress.unsafe(Some("Mr. Me"), "me@example.com"),
  List(
    MailAddress.unsafe(Some("Mr. Mine"), "mine_2@example.com"),
    MailAddress.unsafe(Some("Mr. Me"), "me@example.com"),
    MailAddress.unsafe(Some("Mr. You"), "you@example.com")
  ),
  SSLType.StartTLS,
  MimeType.textHtml
)

val insertRecord = sql"""
  insert into mailaddress(sender, recipients, ssl)
  values(${r.from}, ${r.recipients}, ${r.ssl}, ${r.mime})
""".update.run
```
