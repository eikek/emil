package emil.javamail.internal

import com.sun.mail.imap.IMAPFolder
import cats.implicits._

sealed trait InternalId {

  def asString: String

}

object InternalId {

  case class MessageId(id: String) extends InternalId {
    def asString = s"messageId:$id"
  }
  case class Uid(n: Long) extends InternalId {
    def asString = s"uid:$n"
  }

  def makeInternalId(msg: SafeMimeMessage): InternalId =
    msg.getFolder match {
      case Some(imf: IMAPFolder) =>
        Util.withReadFolder(imf) { _ =>
          InternalId.Uid(imf.getUID(msg.delegate))
        }
      case _ =>
        InternalId.MessageId(msg.getMessageID.getOrElse(sys.error("No message id present")))
    }

  def readInternalId(str: String): Either[String, InternalId] =
    str.split(':').toList match {
      case p :: id :: Nil =>
        if (p == "uid") Either.catchNonFatal(InternalId.Uid(id.toLong)).leftMap(_.getMessage)
        else Either.right(InternalId.MessageId(id))
      case _ =>
        Either.left(s"Invalid id: $str")
    }
}
