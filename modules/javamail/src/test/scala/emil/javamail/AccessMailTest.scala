package emil.javamail

import cats.effect.IO
import emil.{AbstractAccessTest, Emil}
import emil.javamail.internal.JavaMailConnection

object AccessMailTest extends AbstractAccessTest[Unit, JavaMailConnection] {
  def emil: Emil[IO, JavaMailConnection] =
    JavaMailEmil[IO](blocker)

  override def setup(): Unit = ()

}
