import sbt._

object Dependencies {

  val doobieVersion = "0.12.0"
  val flexmarkVersion = "0.62.2"
  val fs2Version = "2.5.3"
  val greenmailVersion = "1.6.2"
  val h2Version = "1.4.200"
  val javaxMailVersion = "1.6.6"
  val jsoupVersion = "1.13.1"
  val log4sVersion = "1.9.0"
  val logbackVersion = "1.2.3"
  val miniTestVersion = "2.9.3"
  val poiVersion = "5.0.0"


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
    "com.sun.mail" % "jakarta.mail" % javaxMailVersion
  )

  val greenmail = Seq(
    "com.icegreen" % "greenmail" % greenmailVersion excludeAll(
      "com.sun.mail" % "javax.mail",
      "junit" % "junit",
      "org.hamcrest" % "hamcrest-core",
      "org.hamcrest" % "hamcrest-all"
    )
  )

  val poi = Seq(
    "org.apache.poi" % "poi" % poiVersion,
  //  "org.apache.poi" % "poi-ooxml" % poiVersion,
    "org.apache.poi" % "poi-scratchpad" % poiVersion,
  ).map(_.excludeAll(
    ExclusionRule("commons-logging")
  ))

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
  ).map(_.excludeAll(
    ExclusionRule("junit"),
    ExclusionRule("hamcrest-core")
  ))

  val jsoup = Seq(
    "org.jsoup" % "jsoup" % jsoupVersion
  )
}
