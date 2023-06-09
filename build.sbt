import com.github.sbt.git.SbtGit.GitKeys._

val scala212 = "2.12.18"
val scala213 = "2.13.11"
val scala3 = "3.3.0"
val updateReadme = inputKey[Unit]("Update readme")

addCommandAlias("ci", "; lint; +test; readme/updateReadme ;microsite/mdoc; +publishLocal")
addCommandAlias(
  "lint",
  "; scalafmtSbtCheck; scalafmtCheckAll; Compile/scalafix --check; Test/scalafix --check"
)
addCommandAlias("fix", "; Compile/scalafix; Test/scalafix; scalafmtSbt; scalafmtAll")

val sharedSettings = Seq(
  organization := "com.github.eikek",
  scalaVersion := scala213,
  scalacOptions ++=
    Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:higherKinds"
    ) ++
      (if (scalaBinaryVersion.value.startsWith("2.12"))
         List(
           "-Xfatal-warnings", // fail when there are warnings
           "-Xlint",
           "-Yno-adapted-args",
           "-Ywarn-dead-code",
           "-Ywarn-unused",
           "-Ypartial-unification",
           "-Ywarn-value-discard"
         )
       else if (scalaBinaryVersion.value.startsWith("2.13"))
         List("-Werror", "-Wdead-code", "-Wunused", "-Wvalue-discard")
       else if (scalaBinaryVersion.value.startsWith("3"))
         List(
           "-explain",
           "-explain-types",
           "-indent",
           "-print-lines",
           "-Ykind-projector",
           "-Xmigration",
           "-Xfatal-warnings"
         )
       else
         Nil),
  crossScalaVersions := Seq(scala212, scala213, scala3),
  Compile / console / scalacOptions := Seq(),
  licenses := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage := Some(url("https://github.com/eikek/emil")),
  versionScheme := Some("early-semver")
) ++ publishSettings

lazy val publishSettings = Seq(
  developers := List(
    Developer(
      id = "eikek",
      name = "Eike Kettner",
      url = url("https://github.com/eikek"),
      email = ""
    )
  ),
  Test / publishArtifact := false
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

val testSettings = Seq(
  libraryDependencies ++= (Dependencies.munit ++
    Dependencies.logback ++
    Dependencies.greenmail).map(_ % Test),
  testFrameworks += new TestFramework("munit.Framework")
)

val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    gitHeadCommit,
    gitHeadCommitDate,
    gitUncommittedChanges,
    gitDescribedVersion
  ),
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoOptions += BuildInfoOption.BuildTime,
  buildInfoPackage := "emil"
)

val scalafixSettings = Seq(
  semanticdbEnabled := true, // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
  ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"
)

lazy val common = project
  .in(file("modules/common"))
  .enablePlugins(BuildInfoPlugin)
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(buildInfoSettings)
  .settings(scalafixSettings)
  .settings(
    name := "emil-common",
    libraryDependencies ++=
      Dependencies.fs2 ++
        Dependencies.fs2io
  )

lazy val javamail = project
  .in(file("modules/javamail"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "emil-javamail",
// consider this option, if there are non-deterministic test outcomes
//    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    Test / fork := true,
    libraryDependencies ++=
      Dependencies.fs2 ++
        Dependencies.fs2io ++
        Dependencies.javaxMail ++
        Dependencies.loggingApi
  )
  .dependsOn(common % "compile->compile;test->test")

lazy val tnef = project
  .in(file("modules/tnef"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "emil-tnef",
    libraryDependencies ++=
      Dependencies.poi
  )
  .dependsOn(common)

lazy val doobie = project
  .in(file("modules/doobie"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "emil-doobie",
    libraryDependencies ++=
      Dependencies.doobie ++
        Dependencies.h2.map(_ % Test)
  )
  .dependsOn(common, javamail)

lazy val markdown = project
  .in(file("modules/markdown"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "emil-markdown",
    libraryDependencies ++=
      Dependencies.flexmark
  )
  .dependsOn(common)

lazy val jsoup = project
  .in(file("modules/jsoup"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "emil-jsoup",
    libraryDependencies ++=
      Dependencies.jsoup
  )
  .dependsOn(common, javamail % "test->compile;test->test")

lazy val microsite = project
  .in(file("modules/microsite"))
  .enablePlugins(MicrositesPlugin, MdocPlugin)
  .settings(sharedSettings)
  .settings(noPublish)
  .settings(
    name := "emil-microsite",
    publishArtifact := false,
    publish / skip := true,
    micrositeFooterText := Some(
      s"""
         |<p>&copy; 2020- <a href="https://github.com/eikek/emil">Emil v${latestRelease.value}</a></p>
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
    run / fork := true,
    scalacOptions := Seq(),
    mdocVariables := Map(
      "VERSION" -> latestRelease.value
    )
  )
  .dependsOn(
    common % "compile->compile,test",
    javamail,
    tnef,
    doobie,
    jsoup /*, markdown*/
  )

lazy val readme = project
  .in(file("modules/readme"))
  .enablePlugins(MdocPlugin)
  .settings(sharedSettings)
  .settings(noPublish)
  .settings(
    name := "emil-readme",
    scalacOptions := Seq(),
    mdocVariables := Map(
      "VERSION" -> latestRelease.value
    ),
    updateReadme := {
      mdoc.evaluated
      val out = mdocOut.value / "readme.md"
      val target = (LocalRootProject / baseDirectory).value / "README.md"
      val logger = streams.value.log
      logger.info(s"Updating readme: $out -> $target")
      IO.copyFile(out, target)
      ()
    }
  )
  .dependsOn(
    common % "compile->compile;compile->test",
    javamail % "compile->compile;compile->test"
  )

val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(noPublish)
  .settings(
    name := "emil-root",
    crossScalaVersions := Nil
  )
  .aggregate(common, javamail, tnef, doobie, markdown, jsoup)
