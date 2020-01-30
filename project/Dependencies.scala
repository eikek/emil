import sbt._

object Dependencies {

  val fs2Version = "2.2.2"
  val greenmailVersion = "1.5.11"
  val javaxMailVersion = "1.6.4"
  val log4sVersion = "1.8.2"
  val logbackVersion = "1.2.3"
  val miniTestVersion = "2.7.0"

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
}
