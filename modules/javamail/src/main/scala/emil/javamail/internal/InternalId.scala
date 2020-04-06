package emil.javamail.internal

import com.sun.mail.imap.IMAPFolder
import cats.implicits._

sealed trait InternalId {

  def asString: String

}

object InternalId {

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
        Util.withReadFolder(imf)(_ => InternalId.Uid(imf.getUID(msg.delegate)))
      case _ =>
        InternalId.MessageId(
          msg.getMessageID.getOrElse(sys.error("No message id present"))
        )
    }

  def readInternalId(str: String): Either[String, InternalId] =
    str.split(':').toList match {
      case p :: id :: Nil =>
        if (p == "uid")
          Either.catchNonFatal(InternalId.Uid(id.toLong)).leftMap(_.getMessage)
        else if (p == "messageId") Either.right(InternalId.MessageId(id))
        else Right(NoId)
      case _ =>
        Either.left(s"Invalid id: $str")
    }
}
