import sbt._

object LatestRelease extends AutoPlugin {
  object autoImport {
    val latestRelease = settingKey[String]("Latest release version")
  }
}
