package emil.javamail

import cats.effect._
import emil._
import emil.javamail.internal.JavaMailConnection

object SendMailTest extends AbstractSendTest[Unit, JavaMailConnection] {

  override def setup(): Unit = ()

  override def tearDown(env: Unit): Unit = ()

  def emil: Emil[IO, JavaMailConnection] =
    JavaMailEmil[IO](blocker)

}
