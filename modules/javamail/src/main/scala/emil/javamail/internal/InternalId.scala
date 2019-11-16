package emil.javamail.internal

import com.sun.mail.imap.IMAPFolder
import javax.mail.internet.MimeMessage
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

  def makeInternalId(msg: MimeMessage): InternalId =
    msg.getFolder match {
      case imf: IMAPFolder =>
        Util.withReadFolder(imf) { _ =>
          InternalId.Uid(imf.getUID(msg))
        }
      case _ =>
        InternalId.MessageId(msg.getMessageID)
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
