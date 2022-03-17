package emil

import munit._

class DispositionTest extends FunSuite {
  test ( "Disposition decoding test with inline" ) {
    val inlined = Disposition.withName("inline")
    assertEquals(inlined, Some(Disposition.Inline))
  }

  test ( "Disposition decoding test with attachment" ) {
    val inlined = Disposition.withName("ATTACHMENT")
    assertEquals(inlined, Some(Disposition.Attachment))
  }

  test ( "Disposition decoding test wrong enum" ) {
    val x = Disposition.withName("x")
    assertEquals(x, Option.empty)
  }

  test ( "Disposition decoding test no value" ) {
    val x = Disposition.withName(null)
    assertEquals(x, Option.empty)
  }
}
