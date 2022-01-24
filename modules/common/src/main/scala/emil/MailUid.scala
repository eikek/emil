package emil

import cats.{Hash, Order, Show}

final case class MailUid(n: Long) {
  override def toString = s"uid:$n"
  def next: MailUid     = MailUid(n + 1)
}

object MailUid extends MailUidLowPriorityImplicits {
  def lastUid: MailUid = MailUid(-1)
  def maxUid: MailUid  = MailUid(0xffffffffL)

  implicit lazy val hash: Hash[MailUid] = Hash.fromUniversalHashCode[MailUid]
  implicit lazy val show: Show[MailUid] = Show.fromToString[MailUid]
}

trait MailUidLowPriorityImplicits {
  implicit lazy val order: Order[MailUid] = Order.by[MailUid, Long](_.n)
}
