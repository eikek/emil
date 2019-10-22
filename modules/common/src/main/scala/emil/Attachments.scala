package emil

final case class Attachments[F[_]](all: Vector[Attachment[F]]) {

  def add(attach: Attachment[F]): Attachments[F] =
    Attachments(attach +: all)

  def isEmpty: Boolean = all.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def size: Int = all.size

  def ++ (next: Attachments[F]): Attachments[F] =
    Attachments(all ++ next.all)
}

object Attachments {
  def empty[F[_]]: Attachments[F] = Attachments(Vector.empty)
  def apply[F[_]](a0: Attachment[F], as: Attachment[F]*): Attachments[F] =
    Attachments(a0 +: as.toVector)
}