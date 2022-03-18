package emil

import munit._

class DispositionTest extends FunSuite {
  test("Disposition decoding test with inline") {
    val inlined = Disposition.fromString("inline").toOption
    assertEquals(inlined, Some(Disposition.Inline))
  }

  test("Disposition decoding test with attachment") {
    val inlined = Disposition.fromString("ATTACHMENT").toOption
    assertEquals(inlined, Some(Disposition.Attachment))
  }

  test("Disposition decoding test wrong enum") {
    val x = Disposition.fromString("x").toOption
    assertEquals(x, Option.empty)
  }

  test("Disposition decoding test no value") {
    val x = Disposition.fromString(null).toOption
    assertEquals(x, Option.empty)
  }

  test("Disposition Either contains") {
    assert(Disposition.fromString("Inline").contains(Disposition.Inline))
    assert(Disposition.fromString("ATTACHMENT").contains(Disposition.Attachment))

    assert(!Disposition.fromString("x").contains(Disposition.Inline))
    assert(!Disposition.fromString(null).contains(Disposition.Inline))
  }

  test("Disposition parsing error") {
    assert(Disposition.fromString("x").isLeft)
    assert(Disposition.fromString(null).isLeft)
  }
}
