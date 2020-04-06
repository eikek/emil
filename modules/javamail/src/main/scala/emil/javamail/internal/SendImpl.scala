package emil.javamail.internal

import cats.effect.{Blocker, ContextShift, Sync}
import cats.data.NonEmptyList
import emil._
import emil.javamail.conv.encode._
import emil.javamail.internal.ops.SendMail
import emil.javamail.internal.BlockingSyntax._

final class SendImpl[F[_]: Sync: ContextShift](blocker: Blocker)
    extends Send[F, JavaMailConnection] {

  def sendMails(
      mails: NonEmptyList[Mail[F]]
  ): MailOp[F, JavaMailConnection, NonEmptyList[String]] =
    SendMail(mails).blockOn(blocker)
}
