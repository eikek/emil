import sbt._

object Dependencies {

  val doobieVersion = "1.0.0-RC2"
  val flexmarkVersion = "0.64.8"
  val fs2Version = "3.7.0"
  val greenmailVersion = "2.0.0"
  val h2Version = "2.1.214"
  val munitVersion = "0.7.29"
  val javaxMailVersion = "2.0.1"
  val jsoupVersion = "1.16.1"
  val log4sVersion = "1.10.0"
  val logbackVersion = "1.2.12"
  val miniTestVersion = "2.9.6"
  val poiVersion = "5.2.3"

  val munit = Seq(
    "org.scalameta" %% "munit" % munitVersion,
    "org.scalameta" %% "munit-scalacheck" % munitVersion
  )

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % fs2Version
  )

  val fs2io = Seq(
    "co.fs2" %% "fs2-io" % fs2Version
  )

  val miniTest = Seq(
    // https://github.com/monix/minitest
    // Apache 2.0
    "io.monix" %% "minitest" % miniTestVersion,
    "io.monix" %% "minitest-laws" % miniTestVersion
  )

  // https://github.com/Log4s/log4s;ASL 2.0
  val loggingApi = Seq(
    "org.log4s" %% "log4s" % log4sVersion
  )

  val logback = Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion
  )

  val javaxMail = Seq(
    "com.sun.mail" % "imap" % javaxMailVersion,
    "com.sun.mail" % "smtp" % javaxMailVersion,
    "com.sun.mail" % "gimap" % javaxMailVersion
  )

  val greenmail = Seq(
    ("com.icegreen" % "greenmail" % greenmailVersion).excludeAll(
      "com.sun.mail" % "javax.mail",
      "com.sun.mail" % "jakarta.mail",
      "junit" % "junit",
      "org.hamcrest" % "hamcrest-core",
      "org.hamcrest" % "hamcrest-all"
    )
  )

  val poi = Seq(
    "org.apache.poi" % "poi" % poiVersion,
    //  "org.apache.poi" % "poi-ooxml" % poiVersion,
    "org.apache.poi" % "poi-scratchpad" % poiVersion
  ).map(
    _.excludeAll(
      ExclusionRule("commons-logging")
    )
  )

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core" % doobieVersion
  )
  val h2 = Seq(
    "com.h2database" % "h2" % h2Version
  )

  val flexmark = Seq(
    "com.vladsch.flexmark" % "flexmark" % flexmarkVersion,
    "com.vladsch.flexmark" % "flexmark-ext-tables" % flexmarkVersion,
    "com.vladsch.flexmark" % "flexmark-ext-gfm-strikethrough" % flexmarkVersion
  ).map(
    _.excludeAll(
      ExclusionRule("junit"),
      ExclusionRule("hamcrest-core")
    )
  )

  val jsoup = Seq(
    "org.jsoup" % "jsoup" % jsoupVersion
  )
}
