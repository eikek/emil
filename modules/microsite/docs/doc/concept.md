---
layout: docs
title: Concept
permalink: doc/concept
---

# {{page.title}}

## Mail Model

Emil uses a simplified model of an e-mail. With
[MIME](https://en.wikipedia.org/wiki/MIME), an e-mail can appear in
various shapes. Emil uses a flat structure, being:

1. Headers
2. Body
3. Attachments

The `Headers` are the standard headers in an e-mail. The `Body`
defines the content of the mail. It can be specified as plain text,
HTML or both. In case both is specified it defines the same content,
just a different format. It is translated into a
`multipart/alternative` message.

Then a list of attachments follows. The major difference to MIME is
that it is not recursive. Emil can only create *Mixed* or
*Alternative* MIME messages.


## MailOp

The other concept is the `MailOp`, which is an alias for the cats
[`Kleisli`](https://typelevel.org/cats/datatypes/kleisli.html) class,
where the input type is a `Connection`. Every code that does something
with an e-mail runs inside such a function.

```
type MailOp[F[_], C, A] = Kleisli[F, C, A]
```

The `C` is the type representing the connection to some mail server.
It is not a concrete type, because this depends on the implementation
and the operations should not depend on it.

There are pre-defined primitive operations in the `Access` and `Send`
trait, respectively. These are implemented by some "implementation
module". These primitive operations can be composed into custom ones.

For example, this is an operation that moves the first mail in INBOX
into the Trash folder:

```scala mdoc
import cats.implicits._, cats.effect._, emil._

def moveToTrash[F[_]: Sync, C](a: Access[F, C]): MailOp[F, C, Unit] = {
  val trash = a.getOrCreateFolder(None, "Trash")
  val findFirstMail = a.getInbox.
    flatMap(in => a.search(in, 1)(SearchQuery.All)).
    map(_.mails.headOption.toRight(new Exception("No mail found."))).
    mapF(_.rethrow)

  for {
    target <- trash
    mail   <- findFirstMail
    _      <- a.moveMail(mail, target)
  } yield ()
}
```

This uses only imports from `emil-common`.


## Implementation

The module `emil-common` lets you define mails and operations among
them. To actually execute these operations, an implementation module
is necessary. Currently there is `emil-javamail`, that is based on the
[JavaMail](https://github.com/eclipse-ee4j/mail) library.

This module provides a factory for creating concrete `Connection`
instances. The `emil-javamail` module has a `ConnectionResource` that
creates a `Resource[F, JavaMailConnection]` given some
`MailConfig`. This can be used to run the `MailOp` operations.

The `Emil` trait exists to make this more convenient. It simply
combines the `ConnectionResource` and the implementations for the
primitive `MailOp`s (defined in `Access` and `Send` trait). The
`emil-javamail` modul provides this as `JavaMailEmil`.

For example, to execute the *"moveToTrash"* operation from above, one
needs a corresponding connection to some imap server and the
`JavaMailEmil`.

```scala mdoc
import emil.javamail._
import scala.concurrent.ExecutionContext

val myemil = JavaMailEmil[IO]()
val imapConf = MailConfig("imap://devmail:143", "dev", "dev", SSLType.NoEncryption)

val moveIO = myemil(imapConf).run(moveToTrash(myemil.access))
// moveIO.unsafeRunSync()
```

Note: The `emil-javamail` depends on the
[JavaMail](https://github.com/eclipse-ee4j/mail) library, which has a
dual license: EPL and GPLv2 with Classpath exception.
