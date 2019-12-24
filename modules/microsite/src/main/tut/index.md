---
layout: home
position: 1
section: home
title: Home
technologies:
 - first: ["Scala", "E-Mail abstraction for Scala"]
---

# E-Mail for Scala

Emil is a library for dealing with E-Mail in Scala. The api builds on
[Cats](https://github.com/typelevel/cats) and
[FS2](https://github.com/functional-streams-for-scala/fs2). It comes
with a backend implementation that is based on the well known [Java
Mail](https://github.com/eclipse-ee4j/mail) library. As such it is
just another wrapper library, but also a bit different:

- Both, accessing and sending mails is supported.
- The data structures model a simplified E-Mail structure. Instead of
  adhering to the recursive structure of a mime message, a mail here
  is flat, consisting of a header, a body (text, html or both) and a
  list of attachments.
- The data structures and api are in a separate module, that doesn't
  depend on a concrete implementation library, like Java Mail. An
  implementation based on
  [fs2-mail](https://github.com/Spinoco/fs2-mail) or even
  [EWS](https://github.com/OfficeDev/ews-java-api) can be created
  without affecting the user code of this library.

So, you write you e-mail related code once and then decide how to
execute.

## Usage

With [sbt](https://scala-sbt.org), add the dependencies:

```
"com.github.eikek" %% "emil-common" % "@VERSION@"  // the core library
"com.github.eikek" %% "emil-javamail" % "@VERSION@" // implementation module
```

Emil is provided for Scala 2.12 and 2.13.

## License

This project is distributed under the
[MIT](https://spdx.org/licenses/MIT)
