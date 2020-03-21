package emil.tnef

import scala.collection.JavaConverters._

object Compat {

  implicit final class ListOps[A](la: java.util.List[A]) {
    def toList: List[A] =
      la.asScala.toList

    def toVector: Vector[A] =
      la.asScala.toVector
  }

}
