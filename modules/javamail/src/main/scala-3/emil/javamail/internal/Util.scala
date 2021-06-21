package emil.javamail.internal

import scala.util.Using

import jakarta.mail.Folder
import jakarta.mail.internet.MimeMessage

object Util {

  def withOpenFolder[A, B <: Folder](f: B, mode: Int)(code: B => A): A =
    if (f.isOpen)
      code(f)
    else
      Using.resource(f) { _ =>
        f.open(mode)
        code(f)
      }

  def withReadFolder[A, B <: Folder](f: B)(code: B => A): A =
    withOpenFolder(f, Folder.READ_ONLY)(code)

  def withWriteFolder[A, B <: Folder](f: B)(code: B => A): A =
    withOpenFolder(f, Folder.READ_WRITE)(code)

  def withReadFolder[A](msg: MimeMessage)(code: MimeMessage => A): A =
    Option(msg.getFolder).map(f => withReadFolder(f)(_ => code(msg))).getOrElse(code(msg))
}
