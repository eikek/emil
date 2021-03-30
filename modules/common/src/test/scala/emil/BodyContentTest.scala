package emil

import java.nio.charset.StandardCharsets

import minitest._
import scodec.bits.ByteVector

object BodyContentTest extends SimpleTestSuite {

  private object CharsetImpl extends Charsets
  val charsets = CharsetImpl.all.values

  test("recover from decoding errors") {
    val bytes = "ö".getBytes(StandardCharsets.UTF_8).take(1)
    val bc    = BodyContent(ByteVector.view(bytes), Some(StandardCharsets.UTF_8))
    assertEquals("Ã", bc.asString)
  }

}
