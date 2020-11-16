package emil.javamail.internal

import java.io.InputStream
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage

import emil.javamail.conv.MessageIdEncode

private[javamail] trait EmilMimeMessage extends MimeMessage {

  def messageIdEncode: MessageIdEncode

  var messageId: Option[String] = None

  override def updateMessageID(): Unit =
    messageIdEncode match {
      case MessageIdEncode.Given =>
        messageId.foreach(id => setHeader("Message-ID", id))
      case MessageIdEncode.Random =>
        super.updateMessageID()
      case MessageIdEncode.GivenOrRandom =>
        messageId match {
          case Some(id) =>
            setHeader("Message-ID", id)
          case None =>
            super.updateMessageID()
        }
    }
}

object EmilMimeMessage {

  def apply(session: Session, midEncode: MessageIdEncode): EmilMimeMessage =
    new MimeMessage(session) with EmilMimeMessage {
      val messageIdEncode = midEncode
    }

  def apply(
      session: Session,
      midEncode: MessageIdEncode,
      is: InputStream
  ): EmilMimeMessage =
    new MimeMessage(session, is) with EmilMimeMessage {
      val messageIdEncode = midEncode
    }
}
