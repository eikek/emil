package emil

trait EmilImap[F[_]] extends Emil[F] {
  def access: AccessImap[F, C]
}
