package emil.javamail.internal

import cats.data.NonEmptyList
import cats.effect.Sync
import emil._
import emil.javamail.conv.encode._
import emil.javamail.internal.ops.SendMail

final class SendImpl[F[_]: Sync] extends Send[F, JavaMailConnection] {

  def sendMails(
      mails: NonEmptyList[Mail[F]]
  ): MailOp[F, JavaMailConnection, NonEmptyList[String]] =
    SendMail(mails)
}
