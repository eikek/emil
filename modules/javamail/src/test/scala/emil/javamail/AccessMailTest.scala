package emil.javamail

import cats.effect.IO
import emil.{AbstractAccessTest, Emil}

class AccessMailTest extends AbstractAccessTest {
  lazy val emil: Emil[IO] =
    JavaMailEmil[IO]()

}
