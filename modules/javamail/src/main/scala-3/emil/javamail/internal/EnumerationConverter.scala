package emil.javamail.internal

import scala.jdk.CollectionConverters._

object EnumerationConverter {

  implicit final class EnumConv[A](jl: java.util.Enumeration[A]) {
    def asScalaList = jl.asScala.toSeq
  }

}
