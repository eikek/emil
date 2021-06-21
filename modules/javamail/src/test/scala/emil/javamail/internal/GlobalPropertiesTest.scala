package emil.javamail.internal

import munit._

class GlobalPropertiesTest extends FunSuite {

  test("set doesn't override existing values") {
    System.setProperty("a.b.c", "yes")
    val old = GlobalProperties.set("a.b.c", "no")
    assertEquals(old, None)
    assertEquals(System.getProperty("a.b.c"), "yes")
  }

  test("set writes new values") {
    System.getProperties().remove("a.b.c")
    assertEquals(System.getProperty("a.b.c"), null)
    val old = GlobalProperties.set("a.b.c", "yes")
    assertEquals(old, Some("a.b.c"))
    assertEquals(System.getProperty("a.b.c"), "yes")
  }

  test("find set with empty") {
    val p = GlobalProperties.findSet("empty")
    assertEquals(p.props, Map.empty[String, String])
  }

  test("find set with lenient") {
    val p = GlobalProperties.findSet("lenient")
    assertEquals(p, GlobalProperties.lenient)
  }

  test("set lenient properties") {
    val _ = GlobalProperties.setAll(GlobalProperties.lenient)
    for (kv <- GlobalProperties.lenient.props) {
      assert(System.getProperty(kv._1) != null)
      assert(System.getProperty(kv._1) != "")
    }
  }
}
