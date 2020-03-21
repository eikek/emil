---
layout: docs
position: 3
title: Documentation
permalink: doc
---

# {{page.title}}

Emil consists of multiple modules. The core is `emil-common`. It
defines the data structures for representing an e-mail and the
`MailOp`, a generic interface for "doing something with it".

There is one implementation module `emil-javamail`, which is currently
based on the [JavaMail]() library.

There are several extension modules, that provide additional features
based on third-party libraries.
