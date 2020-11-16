package emil.javamail.internal

import java.time.Instant
import jakarta.mail._
import jakarta.mail.internet.MimeMessage

import cats.implicits._
import emil.javamail.internal.SafeMimeMessage._
import org.log4s.getLogger

final private[javamail] class SafeMimeMessage(msg: MimeMessage) {
  val delegate = msg

  def getFlags: Option[Flags] =
    getOption("flags", msg.getFlags)

  def getHeader(name: String, delimiter: String): Option[String] =
    getOption(s"getHeader($name, $delimiter)", msg.getHeader(name, delimiter))

  def getHeader(name: String): List[String] =
    getOption(s"getHeader($name)", msg.getHeader(name))
      .map(_.toList)
      .getOrElse(Nil)

  def getSentDate: Option[Instant] =
    getOption("Sent-Date", msg.getSentDate).map(_.toInstant)

  def getMessageID: Option[String] =
    getOption("MessageID", msg.getMessageID)

  def getSubject: Option[String] =
    getOption("Subject", msg.getSubject)

  def getSender: Option[Address] =
    getOption("Sender", msg.getSender)

  def getFolder: Option[Folder] =
    getOption("Folder", msg.getFolder)

  def getFrom: List[Address] =
    getOption("From", msg.getFrom).map(_.toList).getOrElse(Nil)

  def getRecipients(rt: Message.RecipientType): List[Address] =
    getOption(s"Recipients($rt)", msg.getRecipients(rt))
      .map(_.toList)
      .getOrElse(Nil)
}

private[javamail] object SafeMimeMessage {
  private[this] val logger = getLogger

  def apply(mm: MimeMessage): SafeMimeMessage =
    new SafeMimeMessage(mm)

  def getOption[A](name: String, getter: => A): Option[A] =
    Either
      .catchNonFatal(getter)
      .fold(
        _ => {
          logger.debug(s"Error getting '$name' from mime-message. Ignoring this value.");
          None
        },
        Option.apply
      )
}
