package emil.javamail.conv

import javax.mail.Session

trait Conv[A, B] { self =>

  def convert(a: A): B

  final def map[C](f: B => C): Conv[A, C] =
    Conv(a => f(self.convert(a)))

  final def contraMap[C](f: C => A): Conv[C, B] =
    Conv(a => self.convert(f(a)))
}

object Conv {
  def apply[A, B](f: A => B): Conv[A, B] =
    (a: A) => f(a)
}

trait MsgConv[A, B] { self =>

  def convert(session: Session, messageIdEncode: MessageIdEncode, value: A): B

}

object MsgConv {
  def apply[A, B](f: (Session, MessageIdEncode, A) => B): MsgConv[A, B] =
    (session: Session, midEncode: MessageIdEncode, v: A) => f(session, midEncode, v)
}
