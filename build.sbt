
import com.typesafe.sbt.SbtGit.GitKeys._
import xerial.sbt.Sonatype._
import ReleaseTransformations._

val scala212 = "2.12.10"
val scala213 = "2.13.1"

val sharedSettings = Seq(
  organization := "com.github.eikek",
  scalaVersion := scala213,
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
  homepage := Some(url("https://github.com/eikek/emil"))
) ++ publishSettings

lazy val publishSettings = Seq(
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/eikek/emil.git"),
      "scm:git:git@github.com:eikek/emil.git"
    )
  ),
  developers := List(
    Developer(
      id = "eikek",
      name = "Eike Kettner",
      url = url("https://github.com/eikek"),
      email = ""
    )
  ),
  publishArtifact in Test := false,
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    //For non cross-build projects, use releaseStepCommand("publishSigned")
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  sonatypeProjectHosting := Some(GitHubHosting("eikek", "emil", "eike.kettner@posteo.de"))
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
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
  disablePlugins(ReleasePlugin).
  settings(sharedSettings).
  settings(noPublish).
  settings(
    name := "emil-microsite",
    publishArtifact := false,
    skip in publish := true,
    micrositeFooterText := Some(
      """
        |<p>&copy; 2019 <a href="https://github.com/eikek/emil">Emil, v{{site.version}}</a></p>
        |""".stripMargin
    ),
    micrositeName := "Emil",
    micrositeDescription := "Emil – E-Mail library for Scala",
    micrositeFavicons := Seq(microsites.MicrositeFavicon("favicon.png", "35x35")),
    micrositeBaseUrl := "/emil",
    micrositeAuthor := "eikek",
    micrositeGithubOwner := "eikek",
    micrositeGithubRepo := "emil",
    micrositeGitterChannel := false,
    micrositeShareOnSocial := false,
    fork in run := true,
    scalacOptions := Seq(),
    micrositeCompilingDocsTool := WithMdoc,
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    mdocIn := tutSourceDirectory.value
  ).
  dependsOn(common % "compile->compile,test", javamail)

val root = project.in(file(".")).
  settings(sharedSettings).
  settings(noPublish).
  settings(
    name := "emil-root"
  ).
  aggregate(common, javamail)
