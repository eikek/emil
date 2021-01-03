---
layout: docs
title: Obtaining Mails
permalink: doc/reading
---

# {{ page.title }}

The `emil.Access` trait defines primitve `MailOp`s for retrieving
mails and working with folders.

Mails can retrieved by searching a folder. So at first a folder must
be obtained. When searching mails, only `MailHeader`s are returned –
the most common headers of an e-mail. More cane be loaded separately
using the `loadMail` operation.

A `SearchQuery` must be provided to the search operation. Using
`import emil.SearchQuery._` allows for conveniently defining these:

```scala mdoc
import emil._, emil.SearchQuery._
import java.time._

val q = (ReceivedDate >= Instant.now.minusSeconds(60)) && (Subject =*= "test") && !Flagged
```

The subject test is a simple substring test. Look at `SearchQuery` to
find out what is possible.

Searching inbox may look like this:

```scala mdoc
import cats.effect._

def searchInbox[C](a: Access[IO, C], q: SearchQuery) =
  for {
    inbox  <- a.getInbox
    mails  <- a.search(inbox, 20)(q)
  } yield mails
```

The `20` defines a maximum count. Just use `Int.MaxValue` to return
all.

Returning mail headers only from a search saves bandwith and is
fast. If you really want to load all mails found, use the
`searchAndLoad` operation.

```scala mdoc
import cats.effect._

def searchLoadInbox[C](a: Access[IO, C], q: SearchQuery) =
  for {
    inbox  <- a.getInbox
    mails  <- a.searchAndLoad(inbox, 20)(q)
  } yield mails
```
