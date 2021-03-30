package emil.javamail

import cats.effect.IO
import emil.{AbstractAccessTest, Emil}

object AccessMailTest extends AbstractAccessTest[Unit] {
  lazy val emil: Emil[IO] =
    JavaMailEmil[IO](blocker)

  override def setup(): Unit = ()

}
