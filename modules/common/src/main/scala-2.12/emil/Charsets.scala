package emil

import java.nio.charset.Charset

import scala.collection.JavaConverters._

private trait Charsets {
  val all: Map[String, Charset] =
    Charset.availableCharsets().asScala.toMap

  def filterContains(part: String): List[Charset] =
    all.filter(_._1.toLowerCase.contains(part)).values.toList
}
