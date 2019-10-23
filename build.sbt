import com.typesafe.sbt.SbtGit.GitKeys._

val scala212 = "2.12.10"
val scala213 = "2.13.1"

val sharedSettings = Seq(
  organization := "com.github.eikek",
  scalaVersion := scala212,
  scalacOptions ++=
    Seq("-feature",
      "-deprecation",
      "-unchecked",
      "-encoding", "UTF-8",
      "-language:higherKinds") ++
    (if (scalaBinaryVersion.value.startsWith("2.12"))
      List("-Xfatal-warnings", // fail when there are warnings
        "-Xlint",
        "-Yno-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-unused-import",
        "-Ypartial-unification",
        "-Ywarn-value-discard")
    else if (scalaBinaryVersion.value.startsWith("2.13"))
      List("-Werror"
        , "-Wdead-code"
        , "-Wunused"
        , "-Wvalue-discard")
    else
      Nil
    ),
  crossScalaVersions := Seq(scala212, scala213),
  scalacOptions in Test := Seq(),
  scalacOptions in (Compile, console) := Seq(),
  licenses := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage := Some(url("https://github.com/eikek/bitpeace"))
)

val testSettings = Seq(
  testFrameworks += new TestFramework("minitest.runner.Framework"),
  libraryDependencies ++=
    (Dependencies.miniTest ++
      Dependencies.logback ++
      Dependencies.greenmail).
      map(_ % Test)
)

val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, gitHeadCommit, gitHeadCommitDate, gitUncommittedChanges, gitDescribedVersion),
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoOptions += BuildInfoOption.BuildTime
)


lazy val common = project.in(file("modules/common")).
  settings(sharedSettings).
  settings(testSettings).
  settings(
    name := "emil-common",
    libraryDependencies ++=
      Dependencies.fs2 ++ 
      Dependencies.fs2io
  )

lazy val javamail = project.in(file("modules/javamail")).
  settings(sharedSettings).
  settings(testSettings).
  settings(
    name := "emil-javamail",
    libraryDependencies ++=
      Dependencies.fs2 ++
      Dependencies.fs2io ++
      Dependencies.javaxMail ++
      Dependencies.loggingApi
  ).
  dependsOn(common % "compile->compile;test->test")


lazy val microsite = project.in(file("modules/microsite")).
  enablePlugins(MicrositesPlugin).
  settings(sharedSettings).
  settings(
    name := "emil-microsite",
    crossScalaVersions := Seq(),
    publishArtifact := false,
    skip in publish := true,
    scalaVersion := scala212,
    micrositeFooterText := Some(
      """
        |<p>&copy; 2019 <a href="https://github.com/eikek/emil">Emil, v{{site.version}}</a></p>
        |""".stripMargin
    ),
    micrositeName := "Emil",
    micrositeDescription := "Emil – E-Mail library for Scala",
    micrositeFavicons := Seq(microsites.MicrositeFavicon("favicon.png", "96x96")),
    micrositeBaseUrl := "/emil",
    micrositeAuthor := "eikek",
    micrositeGithubOwner := "eikek",
    micrositeGithubRepo := "emil",
    micrositeGitterChannel := false,
    micrositeShareOnSocial := false,
    fork in tut := true,
    scalacOptions in Tut ++=
      Seq("-feature",
        "-deprecation",
        "-unchecked",
        "-encoding", "UTF-8",
        "-language:higherKinds"),
    micrositeCompilingDocsTool := WithTut
  ).
  dependsOn(common % "compile->compile;compile->test", javamail)

val root = project.in(file(".")).
  settings(sharedSettings).
  settings(
    name := "emil-root"
  ).
  aggregate(common, javamail)
