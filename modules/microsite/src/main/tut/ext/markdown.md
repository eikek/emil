---
layout: docs
title: Markdown Bodies
permalink: ext/markdown
---

# {{ page.title }}

The module `emil-markdown` can be used to add markdown mail bodies. It
provides an extension to the mail builder DSL and converts text via
[flexmark](https://github.com/vsch/flexmark-java) into html. The mail
body then contains the text and html version.

## Usage

With sbt:

```
libraryDependencies += "com.github.eikek" %% "emil-markdown" % "@VERSION@"
```


## Description

This module provides a [custom
transformation](../doc/building#custom-transformations) to the builder
dsl to create text and html mail-bodies by using markdown.

```scala
import cats.effect._
import emil._
import emil.builder._
import emil.markdown._

val md = "## Hello!\n\nThis is a *markdown* mail!"

val mail: Mail[IO] = MailBuilder.build(
  From("me@test.com"),
  To("test@test.com"),
  Subject("Hello!"),

  // creates text and html body
  MarkdownBody(md)
)
```

When this mail is send or serialized, the mail body is transformed
into a `multipart/alternative` body with the markdown as a
`text/plain` part and the generated html as a `text/html` part.

Here is how it looks like in roundcube webmail:

<div class="screenshot">
  <img src="../img/20200321-203914.jpg">
</div>
<div class="screenshot">
  <img src="../img/20200321-203916.jpg">
</div>
