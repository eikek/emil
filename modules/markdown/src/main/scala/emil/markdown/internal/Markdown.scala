package emil.markdown.internal

import java.io.{InputStream, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.util

import scala.util.Try

import cats.effect.Sync
import cats.implicits._
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.{DataKey, MutableDataSet}
import emil.markdown.MarkdownConfig
import fs2.Stream

object Markdown {

  def toHtml(is: InputStream, cfg: MarkdownConfig): Either[Throwable, String] = {
    val p = createParser()
    val r = createRenderer()
    Try {
      val reader = new InputStreamReader(is, StandardCharsets.UTF_8)
      val doc = p.parseReader(reader)
      wrapHtml(r.render(doc), cfg)
    }.toEither
  }

  def toHtml(md: String, cfg: MarkdownConfig): String = {
    val p = createParser()
    val r = createRenderer()
    val doc = p.parse(md)
    wrapHtml(r.render(doc), cfg)
  }

  def bytesToHtml[F[_]: Sync](data: Stream[F, Byte], cfg: MarkdownConfig): F[String] =
    stringToHtml(data.through(fs2.text.utf8.decode), cfg)

  def stringToHtml[F[_]: Sync](data: Stream[F, String], cfg: MarkdownConfig): F[String] =
    data.compile.foldMonoid.map(str => toHtml(str, cfg))

  private def wrapHtml(body: String, cfg: MarkdownConfig): String =
    s"""<!DOCTYPE html>
       |<html>
       |<head>
       |<meta charset="utf-8"/>
       |<style>
       |${cfg.internalCss.trim}
       |</style>
       |</head>
       |<body>
       |${body.trim}
       |</body>
       |</html>
       |""".stripMargin

  private def createParser(): Parser = {
    val opts = new MutableDataSet()
    opts.set(
      Parser.EXTENSIONS.asInstanceOf[DataKey[util.Collection[?]]],
      util.Arrays.asList(TablesExtension.create(), StrikethroughExtension.create())
    );

    Parser.builder(opts).build()
  }

  private def createRenderer(): HtmlRenderer = {
    val opts = new MutableDataSet()
    HtmlRenderer.builder(opts).build()
  }
}
