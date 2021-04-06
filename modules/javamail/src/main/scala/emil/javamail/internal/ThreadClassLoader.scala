package emil.javamail.internal

import jakarta.mail.Session

object ThreadClassLoader {

  def apply[A](code: => A): A = {
    val prev = Thread.currentThread().getContextClassLoader
    val next = classOf[Session].getClassLoader
    if (prev eq next) code
    else {
      Thread.currentThread().setContextClassLoader(next)
      try code
      finally Thread.currentThread().setContextClassLoader(prev)
    }
  }

}
