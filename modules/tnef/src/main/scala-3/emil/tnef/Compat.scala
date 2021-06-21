package emil.tnef

import scala.jdk.CollectionConverters._

object Compat {

  implicit final class ListOps[A](la: java.util.List[A]) {
    def toList: List[A] =
      la.asScala.toList

    def toVector: Vector[A] =
      la.asScala.toVector
  }

}
