package emil

trait Send[F[_], C <: Connection] {

  def sendMails(mails: Seq[Mail[F]]): MailOp[F, C, Unit]

  def send(mails: Mail[F]*): MailOp[F, C, Unit] =
    sendMails(mails)

}
