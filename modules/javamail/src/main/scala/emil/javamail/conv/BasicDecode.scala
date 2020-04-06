package emil.javamail.conv

import emil._
import emil.javamail.internal._
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Address, Flags, Folder, Message, internet}

trait BasicDecode {

  implicit def flagDecode: Conv[Flags.Flag, Option[Flag]] =
    Conv(flag => if (flag == Flags.Flag.FLAGGED) Some(Flag.Flagged) else None)

  implicit def folderConv: Conv[Folder, MailFolder] =
    Conv(f => MailFolder(f.getFullName, f.getName))

  implicit def mailAddressDecode: Conv[Address, MailAddress] =
    Conv {
      case a: InternetAddress =>
        MailAddress.unsafe(Option(a.getPersonal), a.getAddress)
      case a =>
        val ia = new internet.InternetAddress(a.toString)
        MailAddress.unsafe(Option(ia.getPersonal), ia.getAddress)
    }

  implicit def mailAddressParse: Conv[String, MailAddress] =
    Conv[String, InternetAddress](str => new InternetAddress(str))
      .map(a => MailAddress.unsafe(Option(a.getPersonal), a.getAddress))

  implicit def recipientsDecode(
      implicit ca: Conv[Address, MailAddress]
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

  implicit def mailHeaderDecode(
      implicit cf: Conv[Folder, MailFolder],
      ca: Conv[Address, MailAddress],
      cr: Conv[MimeMessage, Recipients],
      cs: Conv[String, MailAddress]
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
          replyTo = {
            // msg.getReplyTo method calls getFrom if there is no ReplyTo header
            sm.getHeader("Reply-To", ",").map(cs.convert)
          },
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
