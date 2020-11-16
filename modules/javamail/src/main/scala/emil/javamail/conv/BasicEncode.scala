package emil.javamail.conv

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.util.Date
import jakarta.activation.{DataHandler, DataSource}
import jakarta.mail._
import jakarta.mail.internet._

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import emil._
import emil.javamail.internal.EmilMimeMessage

trait BasicEncode {

  implicit def flagEncode: Conv[Flag, Flags.Flag] =
    Conv { case Flag.Flagged =>
      Flags.Flag.FLAGGED
    }

  implicit def mailAddressEncode: Conv[MailAddress, InternetAddress] =
    Conv({
      case MailAddress(Some(personal), address) =>
        new InternetAddress(address, personal)
      case MailAddress(None, address) =>
        new InternetAddress(address)
    })

  implicit def attachmentEncode[F[_]: Sync]: Conv[Attachment[F], F[MimeBodyPart]] =
    Conv { attach =>
      attach.content.compile.toVector
        .map(_.toArray)
        .map { inData =>
          val part = new MimeBodyPart()
          part.addHeader("Content-Type", attach.mimeType.asString)
          attach.filename.foreach(fn => part.setFileName(MimeUtility.encodeText(fn)))
          part.setDescription("attachment")
          part.setDataHandler(new DataHandler(new DataSource {
            def getInputStream: InputStream =
              new ByteArrayInputStream(inData)

            def getOutputStream: OutputStream =
              sys.error("Not writable")

            def getContentType: String =
              attach.mimeType.asString

            def getName: String =
              attach.filename.orNull
          }))
          part
        }
    }

  implicit def bodyEncode[F[_]: Monad]: Conv[MailBody[F], F[MimeBodyPart]] = {
    def mkTextPart(str: BodyContent): MimeBodyPart = {
      val part = new MimeBodyPart()
      val cs   = str.charsetOrUtf8.name
      part.setText(str.asString, cs.toLowerCase(), "plain")
      part
    }
    def mkHtmlPart(str: BodyContent): MimeBodyPart = {
      val part = new MimeBodyPart()
      val cs   = str.charsetOrUtf8.name
      part.setText(str.asString, cs.toLowerCase(), "html")
      part
    }
    def mkAlternative(txt: BodyContent, html: BodyContent): MimeBodyPart = {
      val p = new MimeMultipart("alternative")
      p.addBodyPart(mkTextPart(txt))
      p.addBodyPart(mkHtmlPart(html))
      val b = new MimeBodyPart()
      b.setContent(p)
      b
    }

    Conv({
      case MailBody.Empty() =>
        mkTextPart(BodyContent.empty).pure[F]
      case MailBody.Text(txt) =>
        txt.map(mkTextPart)
      case MailBody.Html(html) =>
        html.map(mkHtmlPart)
      case MailBody.HtmlAndText(txt, html) =>
        for {
          tp <- txt
          hp <- html
        } yield mkAlternative(tp, hp)
    })
  }

  implicit def mailHeaderEncode(implicit
      ca: Conv[MailAddress, InternetAddress],
      cf: Conv[Flag, Flags.Flag]
  ): MsgConv[MailHeader, MimeMessage] =
    MsgConv { (session, midEncode, header) =>
      val msg = EmilMimeMessage(session, midEncode)

      header.from.map(ca.convert).foreach(msg.setFrom)
      header.sender.map(ca.convert).foreach(msg.setSender)
      msg.setSubject(header.subject)
      header.originationDate.foreach(i => msg.setSentDate(Date.from(i)))
      msg.messageId = header.messageId
      msg.setRecipients(
        Message.RecipientType.TO,
        header.recipients.to.map(ca.convert).map(a => a.asInstanceOf[Address]).toArray
      )
      msg.setRecipients(
        Message.RecipientType.CC,
        header.recipients.cc.map(ca.convert).map(a => a.asInstanceOf[Address]).toArray
      )
      msg.setRecipients(
        Message.RecipientType.BCC,
        header.recipients.bcc.map(ca.convert).map(a => a.asInstanceOf[Address]).toArray
      )

      if (header.flags.nonEmpty) {
        val flags = new Flags()
        header.flags.map(cf.convert).foreach(flags.add)
        msg.setFlags(flags, true)
      }

      msg
    }

  implicit def mailEncode[F[_]: Sync](implicit
      ch: MsgConv[MailHeader, MimeMessage],
      cb: Conv[MailBody[F], F[MimeBodyPart]],
      ca: Conv[Attachment[F], F[MimeBodyPart]]
  ): MsgConv[Mail[F], F[MimeMessage]] = {

    def assemble(
        mail: Mail[F],
        msg: MimeMessage,
        body: MimeBodyPart,
        attachs: Vector[MimeBodyPart]
    ): Unit = {
      mail.additionalHeaders.all
        .filter(h => h.noneOf("Content-Type", "MIME-Version"))
        .foreach(h => h.value.toList.foreach(v => msg.addHeader(h.name, v)))
      if (attachs.nonEmpty) {
        val content = new MimeMultipart()
        content.addBodyPart(body)
        attachs.foreach(content.addBodyPart)
        msg.setContent(content)
      } else {
        val ct =
          Option(body.getDataHandler).map(_.getContentType).getOrElse(body.getContentType)
        msg.setContent(body.getContent, ct)
      }
      // see https://javaee.github.io/javamail/FAQ#hdrs
      msg.saveChanges()
    }

    MsgConv { (session, midEncode, mail) =>
      for {
        attachs <- mail.attachments.all.traverse(ca.convert)
        mbody   <- cb.convert(mail.body)
        msg = ch.convert(session, midEncode, mail.header)
        _   = assemble(mail, msg, mbody, attachs)
      } yield msg
    }
  }
}
