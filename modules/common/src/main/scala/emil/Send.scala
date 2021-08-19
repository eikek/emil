package emil
import cats.data.NonEmptyList

trait Send[F[_], C] {

  /** Return an operation that sends a list of mails and returns the generated messageIDs.
    */
  def sendMails(mails: NonEmptyList[Mail[F]]): MailOp[F, C, NonEmptyList[String]]

  def send(mail: Mail[F], mails: Mail[F]*): MailOp[F, C, NonEmptyList[String]] =
    sendMails(NonEmptyList.of(mail, mails: _*))

}
