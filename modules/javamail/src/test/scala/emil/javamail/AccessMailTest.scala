package emil.javamail

import cats.effect.IO
import emil.AbstractAccessTest

class AccessMailTest extends AbstractAccessTest(JavaMailEmil[IO]()) {}
