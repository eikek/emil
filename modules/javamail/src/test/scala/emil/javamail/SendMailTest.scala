package emil.javamail

import cats.effect._
import emil._

class SendMailTest extends AbstractSendTest(JavaMailEmil[IO]()) {}
