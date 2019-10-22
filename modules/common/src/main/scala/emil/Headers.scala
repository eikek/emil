package emil

final case class Headers(all: List[Header]) {

  def add(h: Header): Headers = {
    val next = all.map { eh =>
      if (eh.name.equalsIgnoreCase(h.name)) eh.append(h.value.toList)
      else eh
    }
    if (next == all) Headers(all :+ h)
    else Headers(next)
  }

  def size: Int = all.size

  def find(name: String): Option[Header] =
    all.find(_.name.equalsIgnoreCase(name))
}

object Headers {
  val empty: Headers = Headers(Nil)

  def apply(hdrs: Header*): Headers =
    Headers(hdrs.toList)
}
