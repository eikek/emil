---
layout: docs
title: Jsoup
permalink: ext/jsoup
---

# {{ page.title }}

The module `emil-jsoup` can be used for easier dealing with html
mails. It uses the famous [jsoup](https://jsoup.org) library to read
the html.

## Usage

With sbt:

```
libraryDependencies += "com.github.eikek" %% "emil-jsoup" % "@VERSION@"
```


## Description

This module provides a [custom
transformation](../doc/building#custom-transformations) to the builder
dsl to modify the html content in an e-mail.

- Clean the html from unwanted content using a custom whitelist of
  allowed elements. For more information, please refer to the
  documentation of [jsoup](https://jsoup.org).
- Create a informative html view of a mail body.

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
cleanMail.body.htmlPart.map(_.map(_.asString)).unsafeRunSync
```

Jsoup even fixes the invalid html tree.


### Html View

The `HtmlBodyView` class can be used to create a unified view of an
e-mail body. It creates HTML and converts a single text/plain part
into html. For better results here, use the `emil-markdown` module.


```scala mdoc
val htmlView = HtmlBodyView(
  mail.body,
  Some(mail.header),
  None, //here the emil-markdown module can be used to convert text->html
  Some(BodyClean.whitelistClean(EmailWhitelist.default))
)
```

If the `mailHeader` is given, the html contains a short header with
the sender, receiver and subject. Then a function can be given to
convert a text-only body into html. If it is omitted a very basic
default conversion is used. At last, another function can be specified
to further modify the html content. This would be a good place to
clean the html from unwanted content.

This is the outcome:

```scala mdoc
htmlView.map(_.asString).unsafeRunSync
```
