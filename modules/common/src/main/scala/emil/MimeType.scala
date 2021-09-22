package emil

import java.nio.charset.{Charset, StandardCharsets}

import cats.Hash

final case class MimeType(
    primary: String,
    sub: String,
    params: Map[String, String]
) {

  def withParam(name: String, value: String): MimeType =
    copy(params = params.updated(name, value))

  def withCharset(cs: Charset): MimeType =
    withParam("charset", cs.name())

  def withUtf8Charset: MimeType =
    withCharset(StandardCharsets.UTF_8)

  def baseType: MimeType =
    if (params.isEmpty) this else copy(params = Map.empty)

  def asString: String =
    if (params.isEmpty) s"$primary/$sub"
    else {
      val parameters = params.toList.map(t => s"${t._1}=${t._2}").mkString(";")
      s"$primary/$sub; $parameters"
    }
}

object MimeType {

  val octetStream = application("octet-stream")
  val textHtml = text("html").withUtf8Charset
  val textPlain = text("plain").withUtf8Charset
  val pdf = application("pdf")

  def apply(primary: String, sub: String): MimeType =
    MimeType(primary, sub, Map.empty)

  def application(sub: String): MimeType =
    apply("application", sub)

  def text(sub: String): MimeType =
    apply("text", sub)

  implicit lazy val hash: Hash[MimeType] = Hash.fromUniversalHashCode[MimeType]

}
