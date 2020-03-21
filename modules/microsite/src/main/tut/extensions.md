---
layout: docs
position: 4
title: Extension Modules
permalink: extensions
---

# {{ page.title }}

This is a list of extension modules to emil. They add more features or
provide integration into on other libraries.

To use them, you need to add them to your build. With sbt:

```
libraryDependencies += "com.github.eikek" %% "emil-{name}" % "@VERSION@"
```


- [TNEF](ext/tnef) Extract TNEF attachments (a.k.a.
  `winmail.dat`).
- [Doobie](ext/doobie) Provides meta instances for some emil types for
  Doobie.
- [Markdown](ext/markdown) Use markdown for your mail bodies to create
  a text and html version.
