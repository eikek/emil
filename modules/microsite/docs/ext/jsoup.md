---
layout: docs
title: Jsoup
permalink: ext/jsoup
---

# {{ page.title }}

The module `emil-jsoup` can be used for easier dealing with html
mails. It is based on the famous [jsoup](https://jsoup.org) library.

## Usage

With sbt:

```
libraryDependencies += "com.github.eikek" %% "emil-jsoup" % "@VERSION@"
```


## Description

This module provides the following:

- A [custom transformation](../doc/building#custom-transformations) to
  the builder dsl to modify the html content in an e-mail. This allows
  to clean the html from unwanted content using a custom whitelist of
  allowed elements. For more information, please refer to the
  documentation of [jsoup](https://jsoup.org).
- Create a unified html view of a mail body.


For the examples, consider the following mail:

```scala mdoc
import cats.effect._
import emil._
import emil.builder._

val htmlMail = """<h1>A header</h2><p onclick="alert('hi!');">Hello</p><p>World<p>"""

val mail: Mail[IO] = MailBuilder.build(
  From("me@test.com"),
  To("test@test.com"),
  Subject("Hello!"),
  HtmlBody(htmlMail)
)
```

### Cleaning HTML

Note the evil `onclick` and the malformed html. A clean content can be
created using the `BodyClean`
[transformation](../doc/building#custom-transformations):

```scala mdoc
import emil.jsoup._

val cleanMail = mail.asBuilder
  .add(BodyClean(EmailWhitelist.default))
  .build
```

This creates a new mail where the body is annotated with a cleaning
function. This only applies to html parts. When the body is now
evaluated, the string looks now like this:

```scala mdoc
import cats.effect.unsafe.implicits.global

cleanMail.body.htmlPart.map(_.map(_.asString)).unsafeRunSync()
```

Jsoup even fixes the invalid html tree.


### Html View

The `HtmlBodyView` class can be used to create a unified view of an
e-mail body. It produces HTML, converting a text-only body into html.
For better results here, use the `emil-markdown` module.

Example:

```scala mdoc
val htmlView = HtmlBodyView(
  mail.body,
  Some(mail.header)
)
```

If the `mailHeader` is given (second argument), a short header with
the sender, receiver and subject is included into the result. The
third argument is a config object `HtmlBodyViewConfig` that has a
default value that contains:

- a function to convert a text-only body into html. This uses a very
  basic string replacement approach and also escapes html entities in
  the text. Use the `emil-markdown` module for more sophisticated
  text-to-html conversion.
- a datetime-formatter and a timezone to use when inserting the e-mail
  date into the document
- a function to modify the html document tree, which by defaults uses
  the cleaner from `BodyClean` to remove unwanted content

The result of the example is:

```scala mdoc
htmlView.map(_.asString).unsafeRunSync()
```
