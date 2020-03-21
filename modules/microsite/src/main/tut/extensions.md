---
layout: docs
position: 4
title: Extension Modules
permalink: extensions
---

# {{ page.title }}

This is a list of extension modules to emil. They add more features
that are based on other libraries or they provide integration with
other libraries.

To use them, you need to add them to your build. With sbt:

```
libraryDependencies += "com.github.eikek" %% "emil-{name}" % "@VERSION@"
```

- [TNEF Support](ext/tnef) Extract TNEF attachments (a.k.a.
  `winmail.dat`).
