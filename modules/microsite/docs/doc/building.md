---
layout: docs
title: Creating Mails
permalink: doc/building
---

# {{ page.title }}

The `Mail` class in `emil-common` is used to represent an
e-mail. Using the `MailBuilder` makes creating values more
convenient. The `MailBuilder` works by collecting a list of
transformations to an initial `Mail` object and applying them when
invoking the `build` method. There exists a set of predefined
transformations to cover the most common things. But you can create
your own easily, too.

## Simple Mails

Creating mails without attachments:

```scala mdoc
import cats.effect._, emil._, emil.builder._

val mail: Mail[IO] = MailBuilder.build(
  From("me@test.com"),
  To("test@test.com"),
  Subject("Hello!"),
  CustomHeader(Header("User-Agent", "my-email-client")),
  TextBody("Hello!\n\nThis is a mail."),
  HtmlBody("<h1>Hello!</h1>\n<p>This <b>is</b> a mail.</p>")
)
```

The `Mail` defines an effect type, because the attachments and the
mail body does. Using `MailBuilder.build` already applies all
transformations and yields the final `Mail` instance. Using the
`MailBuilder.apply` instead, would return the `MailBuilder` instance
which can be further modified. It is also possible to go from an
existing `Mail` to a `MailBuilder` to change certain parts:

```scala mdoc
val builder = mail.asBuilder
val mail2 = builder.
  clearRecipients.
  add(To("me2@test.com")).
  set(Subject("Hello 2")).
  build
```

The `add` and `set` methods are both doing the same thing: appending
transfomations. Both names exists for better reading; i.e. a recipient
is by default appended, but the subject is not. The methods accept a
list of transformations, too.

```scala mdoc
val mail3 = mail2.asBuilder.
  clearRecipients.
  add(
    To("me3@test.com"),
    Cc("other@test.com")
  ).
  build
```


## Mails with Attachments

Adding attachments is the same as with other data. Creating
attachemnts might be more involved, depending on where the data is
coming from. Emil defines transformations to add attachments from
files, urls and java's `InputStream` easily. Otherwise you need to get
a `Stream[F, Byte]` from somewhere else.

```scala mdoc
import scala.concurrent.ExecutionContext

implicit val CS = IO.contextShift(ExecutionContext.global)
val blocker = Blocker.liftExecutionContext(ExecutionContext.fromExecutorService(java.util.concurrent.Executors.newCachedThreadPool()))

val mail4 = mail3.asBuilder.add(
  AttachUrl[IO](getClass.getResource("/files/Test.pdf"), blocker).
    withFilename("test.pdf").
    withMimeType(MimeType.pdf)).
  build
```

Emil creates a `Stream[F, Byte]` from the `java.net.URL` using the
[fs2-io](https://github.com/functional-streams-for-scala/fs2/tree/master/io/src)
api, that requires a `Blocker` and a `ContextShift`. The same applies
when attaching files and `java.io.InputStream`s.

Attaching any given `Stream[F, Byte]` is the same. It doesn't require
`Blocker` or `ContextShift` then.

```scala mdoc
import fs2.Stream

val mydata: Stream[IO, Byte] = Stream.empty.covary[IO]

val mail5 = mail4.asBuilder.
  clearAttachments.
  add(
    Attach(mydata).
      withFilename("empty.txt")
  ).
  build
```


## Custom Transformations

Customs transformations can be easily created. It's only a function
`Mail[F] => Mail[F]`. This can be handy, if there is a better way to
retrieve attachments, or just to create common headers.

```scala mdoc
object MyHeader {
  def apply[F[_]](value: String): Trans[F] =
    CustomHeader("X-My-Header", value)
}

val mymail = MailBuilder.build[IO](
  To("me@test.com"),
  MyHeader("blablabla"),
  TextBody("This is a text")
)
```

The above example simply delegates to an existing `Trans` constructor.
