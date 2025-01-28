package emil.javamail.internal

import cats.implicits._
import com.sun.mail.imap.IMAPFolder
import org.log4s.getLogger

sealed trait InternalId {

  def asString: String

}

object InternalId {
  private val logger = getLogger

  case class MessageId(id: String) extends InternalId {
    val asString = s"messageId:$id"
  }
  case class Uid(n: Long) extends InternalId {
    val asString = s"uid:$n"
  }
  case object NoId extends InternalId {
    val asString = "no-id:no-id"
  }

  def makeInternalId(msg: SafeMimeMessage): InternalId =
    msg.getFolder match {
      case Some(imf: IMAPFolder) =>
        Util.withReadFolder(imf)(_ => Uid(imf.getUID(msg.delegate)))
      case _ =>
        msg.getMessageID match {
          case Some(mid) =>
            MessageId(mid)
          case _ =>
            logger.info(s"Mail '${msg.getSubject}' has no message id")
            NoId
        }
    }

  def readInternalId(str: String): Either[String, InternalId] =
    str.split(':').toList match {
      case p :: id :: Nil =>
        if (p == "uid")
          Either.catchNonFatal(Uid(id.toLong)).leftMap(_.getMessage)
        else if (p == "messageId") Either.right(MessageId(id))
        else Right(NoId)
      case _ =>
        Either.left(s"Invalid id: $str")
    }
}
