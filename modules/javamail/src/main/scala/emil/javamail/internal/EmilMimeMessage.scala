package emil.javamail.internal

import java.io.InputStream

import emil.javamail.conv.MessageIdEncode
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage

private[javamail] trait EmilMimeMessage extends MimeMessage {

  def messageIdEncode: MessageIdEncode

  def messageId: Option[String]

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

  def apply(
      session: Session,
      midEncode: MessageIdEncode,
      mid: Option[String]
  ): EmilMimeMessage =
    new MimeMessage(session) with EmilMimeMessage {
      val messageIdEncode = midEncode
      val messageId = mid
    }

  def apply(
      session: Session,
      midEncode: MessageIdEncode,
      is: InputStream,
      mid: Option[String]
  ): EmilMimeMessage =
    new MimeMessage(session, is) with EmilMimeMessage {
      val messageIdEncode = midEncode
      val messageId = mid
    }
}
