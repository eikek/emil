package emil

import minitest._

object HeaderTest extends SimpleTestSuite {

  test ("replace headers") {
    val hdrs = Headers(Header("X-Name", "bla"))
    val hdrs2 = hdrs.add(Header("X-Name", "alb"))
    assertEquals(hdrs2.size, hdrs.size)
    assertEquals(hdrs2.all.head, Header("X-Name", "bla", "alb"))
  }

  test ("add headers") {
    val hdrs = Headers(Header("X-Name", "bla"))
    val hdrs2 = hdrs.add(Header("X-Test", "bla"))
    assertEquals(hdrs2.size, 2)
    assertEquals(hdrs2.all.head, Header("X-Name", "bla"))
    assertEquals(hdrs2.all(1), Header("X-Test", "bla"))
  }
}
