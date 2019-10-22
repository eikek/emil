package emil.javamail.internal

import javax.mail.internet.MimeMessage

object ThreadClassLoader {

  def apply[A](code: => A): A = {
    val cl = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(classOf[MimeMessage].getClassLoader)
    try {
      code
    } finally {
      Thread.currentThread().setContextClassLoader(cl)
    }
  }

}
