---
layout: docs
title: Extract TNEF files
permalink: ext/tnef
---

# {{ page.title }}

The module `emil-tnef` can be used to extract `winmail.dat`
([tnef](https://en.wikipedia.org/wiki/Transport_Neutral_Encapsulation_Format)
files) that may occur as attachments.

## Usage

With sbt:

```
libraryDependencies += "com.github.eikek" %% "emil-tnef" % "@VERSION@"
```


## Description

It happens that some clients add an attachment, often called
`winmail.dat`, that contains the complete message, or some
attachments. Fortunately, the [poi](http://poi.apache.org/) library
can read these files. The `emil-tnef` module provides a convenient way
to extract these attachments.


```scala mdoc:silent
import cats.effect._
import fs2.Stream
import emil._
import emil.builder._
import emil.tnef._

// creating a mail with a TNEF attachment
val winmailData: Stream[IO, Byte] = Stream.empty
val mail = MailBuilder.build[IO](
  From("me@example.com"),
  To("me@example.com"),
  Subject("test"),
  TextBody[IO]("hello"),
  AttachStream[IO](winmailData, Some("winmail.dat"), TnefMimeType.applicationTnef)
)

// replaces each TNEF attachment with its content
val mail2: IO[Mail[IO]] = TnefExtract.replace[IO](mail)
// mail2.unsafeRunSync()
```

The extraction must read in the tnef byte stream, therefore the return
value is inside a `F`.
