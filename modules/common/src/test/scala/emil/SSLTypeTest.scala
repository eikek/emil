package emil

import munit._

class SSLTypeTest extends FunSuite {

  test("to name") {
    assertEquals(SSLType.SSL.name, "ssl")
    assertEquals(SSLType.StartTLS.name, "starttls")
    assertEquals(SSLType.NoEncryption.name, "noencryption")
  }

  test("from name") {
    assertEquals(SSLType.unsafe("ssl"), SSLType.SSL)
    assertEquals(SSLType.unsafe("SSL"), SSLType.SSL)
    assertEquals(SSLType.unsafe("starttls"), SSLType.StartTLS)
    assertEquals(SSLType.unsafe("startTLS"), SSLType.StartTLS)
    assertEquals(SSLType.unsafe("none"), SSLType.NoEncryption)
    assertEquals(SSLType.unsafe("Noencryption"), SSLType.NoEncryption)
  }
}
