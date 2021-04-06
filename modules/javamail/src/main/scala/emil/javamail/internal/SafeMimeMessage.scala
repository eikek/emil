package emil.javamail.internal

import java.time.Instant

import cats.implicits._
import emil.javamail.internal.SafeMimeMessage._
import jakarta.mail._
import jakarta.mail.internet.MimeMessage
import org.log4s.getLogger

final private[javamail] class SafeMimeMessage(msg: MimeMessage) {
  val delegate = msg

  def getFlags: Option[Flags] =
    getOptionA("flags", msg.getFlags)

  def getHeader(name: String, delimiter: String): Option[String] =
    getOption(s"getHeader($name, $delimiter)", msg.getHeader(name, delimiter))

  def getHeader(name: String): List[String] =
    getOptionA(s"getHeader($name)", msg.getHeader(name))
      .map(_.toList)
      .getOrElse(Nil)

  def getSentDate: Option[Instant] =
    getOptionA("Sent-Date", msg.getSentDate).map(_.toInstant)

  def getMessageID: Option[String] =
    getOption("MessageID", msg.getMessageID)

  def getSubject: Option[String] =
    getOption("Subject", msg.getSubject)

  def getSender: Option[Address] =
    getOptionA("Sender", msg.getSender)

  def getFolder: Option[Folder] =
    getOptionA("Folder", msg.getFolder)

  def getFrom: List[Address] =
    getOptionA("From", msg.getFrom).map(_.toList).getOrElse(Nil)

  def getRecipients(rt: Message.RecipientType): List[Address] =
    getOptionA(s"Recipients($rt)", msg.getRecipients(rt))
      .map(_.toList)
      .getOrElse(Nil)
}

private[javamail] object SafeMimeMessage {
  private[this] val logger = getLogger

  def apply(mm: MimeMessage): SafeMimeMessage =
    new SafeMimeMessage(mm)

  def getOptionA[A](name: String, getter: => A): Option[A] =
    Either
      .catchNonFatal(getter)
      .fold(
        _ => {
          logger.debug(s"Error getting '$name' from mime-message. Ignoring this value.");
          None
        },
        Option.apply
      )

  def getOption(name: String, getter: => String): Option[String] =
    getOptionA(name, getter).map(_.trim).filter(_.nonEmpty)
}
