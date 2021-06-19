package emil.javamail

import cats.effect._
import emil._

object SendMailTest extends AbstractSendTest[Unit] {

  override def setup(): Unit = ()

  override def tearDown(env: Unit): Unit = ()

  lazy val emil: Emil[IO] =
    JavaMailEmil[IO]()

}
