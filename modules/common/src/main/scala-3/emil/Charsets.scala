package emil

import java.nio.charset.Charset

import scala.jdk.CollectionConverters._

private trait Charsets {
  val all: Map[String, Charset] =
    Charset.availableCharsets().asScala.toMap

  def filterContains(part: String): List[Charset] =
    all.view.filterKeys(_.toLowerCase.contains(part)).values.toList
}
