package emil.javamail.conv

import scala.language.postfixOps

import cats.data.{NonEmptyList, Validated, ValidatedNec}
import cats.implicits._
import emil._
import emil.javamail.internal._
import jakarta.mail._
import jakarta.mail.internet.{AddressException, InternetAddress, MimeMessage}

trait BasicDecode {

  implicit def flagDecode: Conv[Flags.Flag, Option[Flag]] =
    Conv(flag => if (flag == Flags.Flag.FLAGGED) Some(Flag.Flagged) else None)

  /** The full name of the folder should never be empty, therefore
    * NonEmptyList.fromListUnsafe is used
    */
  implicit def folderConv: Conv[Folder, MailFolder] =
    Conv(f =>
      MailFolder(
        f.getFullName,
        NonEmptyList.fromListUnsafe(f.getFullName.split(f.getSeparator).toList)
      )
    )

  implicit def mailAddressDecode: Conv[Address, MailAddress] =
    Conv {
      case a: InternetAddress =>
        MailAddress.unsafe(Option(a.getPersonal), a.getAddress)
      case a =>
        val ia = new internet.InternetAddress(a.toString)
        MailAddress.unsafe(Option(ia.getPersonal), ia.getAddress)
    }

  implicit def mailAddressParse: Conv[String, Either[String, MailAddress]] =
    Conv(str =>
      Either
        .catchNonFatal(new InternetAddress(str, true))
        .leftMap(ex => s"Invalid mail address '$str' - ${ex.getMessage}")
        .map(a => MailAddress.unsafe(Option(a.getPersonal), a.getAddress))
    )

  implicit def mailAddressParseValidated
      : Conv[String, ValidatedNec[AddressException, MailAddress]] =
    Conv(str =>
      Validated
        .catchOnly[AddressException](new InternetAddress(str, true))
        .toValidatedNec
        .map[MailAddress](a => MailAddress.unsafe(Option(a.getPersonal), a.getAddress))
    )

  implicit def mailAddressParseNameAndAddress
      : Conv[(Option[String], String), Either[String, MailAddress]] =
    mailAddressParse.contraMap[(Option[String], String)](
      (MailAddress.twoPartDisplay _).tupled
    )

  implicit def mailAddressParseNameAndAddressValidated
      : Conv[(Option[String], String), ValidatedNec[AddressException, MailAddress]] =
    mailAddressParseValidated.contraMap[(Option[String], String)](
      (MailAddress.twoPartDisplay _).tupled
    )

  implicit def recipientsDecode(implicit
      ca: Conv[Address, MailAddress]
  ): Conv[MimeMessage, Recipients] = {
    def recipients(msg: MimeMessage, t: Message.RecipientType): List[Address] =
      SafeMimeMessage(msg).getRecipients(t)

    Conv(msg =>
      Recipients(
        recipients(msg, Message.RecipientType.TO).map(ca.convert),
        recipients(msg, Message.RecipientType.CC).map(ca.convert),
        recipients(msg, Message.RecipientType.BCC).map(ca.convert)
      )
    )
  }

  implicit def mailHeaderDecode(implicit
      cf: Conv[Folder, MailFolder],
      ca: Conv[Address, MailAddress],
      cr: Conv[MimeMessage, Recipients],
      cs: Conv[String, Either[String, MailAddress]]
  ): Conv[MimeMessage, MailHeader] =
    Conv(msg =>
      Util.withReadFolder(msg) { _ =>
        val sm = SafeMimeMessage(msg)
        emil.MailHeader(
          id = InternalId.makeInternalId(sm).asString,
          messageId = sm.getMessageID,
          folder = sm.getFolder.map(cf.convert),
          recipients = cr.convert(msg),
          sender = sm.getSender.map(ca.convert),
          from = sm.getFrom.headOption.map(ca.convert),
          replyTo =
            // msg.getReplyTo method calls getFrom if there is no ReplyTo header
            sm.getHeader("Reply-To", ",").flatMap(cs.map(_.toOption).convert),
          originationDate = sm.getSentDate,
          subject = sm.getSubject.getOrElse(""),
          sm.getHeader("Received").flatMap(r => Received.parse(r).toOption),
          flags =
            if (sm.getFlags.exists(_.contains(Flags.Flag.FLAGGED))) Set(Flag.Flagged)
            else Set.empty
        )
      }
    )
}
